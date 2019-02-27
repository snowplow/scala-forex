/*
 * Copyright (c) 2013-2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.forex

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.math.{BigDecimal, RoundingMode}

import scala.util.{Failure, Success, Try}

import cats.effect.Sync
import cats.implicits._
import cats.data.{EitherT, OptionT}
import org.joda.money._

import oerclient._

/** Companion object to get Forex object */
object Forex {

  /**
   * Fields for calculating currency rates conversions.
   * Usually the currency rate has 6 decimal places
   */
  val commonScale = 6

  /** Helper method to get the ratio of from:to in BigDecimal type */
  def getForexRate(
    fromCurrIsBaseCurr: Boolean,
    baseOverFrom: BigDecimal,
    baseOverTo: BigDecimal
  ): BigDecimal =
    if (!fromCurrIsBaseCurr) {
      val fromOverBase = new BigDecimal(1).divide(baseOverFrom, Forex.commonScale, RoundingMode.HALF_EVEN)
      fromOverBase.multiply(baseOverTo)
    } else {
      baseOverTo
    }

  def getForex[F[_]: Sync](config: ForexConfig): F[Forex[F]] =
    ForexClient
      .getClient[F](config)
      .map(client => Forex(config, client))
}

/**
 * Starts building the fluent interface for currency look-up and conversion,
 * Forex class has methods rate and convert, which set the source currency to
 * the currency we are interested in.
 * They return ForexLookupTo object which is then passed to methods
 * in ForexLookupTo class in the fluent interface.
 * @param config A configurator for Forex object
 * @param client Passed down client that does actual work
 */
case class Forex[F[_]](config: ForexConfig, client: ForexClient[F]) {

  def rate: ForexLookupTo[F] = ForexLookupTo(1, config.baseCurrency, config, client)

  /**
   * Starts building a fluent interface, performs currency look up from *source* currency.
   * (The target currency will be supplied to the ForexLookupTo later).
   * @param currency Source currency
   * @return ForexLookupTo object which is the part of the fluent interface
   */
  def rate(currency: CurrencyUnit): ForexLookupTo[F] =
    ForexLookupTo(1, currency, config, client)

  /**
   * Starts building a currency conversion from the supplied currency, for the supplied amount.
   * @param amount The amount of currency to be converted
   * @param currency CurrencyUnit to convert from
   * @return a ForexLookupTo, part of the currency conversion fluent interface.
   */
  def convert(amount: Double, currency: CurrencyUnit): ForexLookupTo[F] =
    convert(amount, currency)

  /**
   * Starts building a currency conversion for the supplied amount, using the currency specified in config
   * @param amount The amount of currency to be converted
   * @return a ForexLookupTo, part of the currency conversion fluent interface
   */
  def convert(amount: Double): ForexLookupTo[F] =
    ForexLookupTo(amount, config.baseCurrency, config, client)

}

/**
 * ForexLookupTo is the second part of the fluent interface,
 * which passes parameters taken from Forex class to the next stage in the fluent interface
 * and sets the target currency for the lookup/conversion.
 * Method in this class returns ForexLookupWhen object which will be passed to the next stage
 * in the fluent interface.
 * @param conversionAmount The amount of money to be converted,
 * it is set to 1 unit for look up operation.
 * @param fromCurr The source currency
 * @param config Forex config
 * @param client Passed down client that does actual work
 */
case class ForexLookupTo[F[_]](
  conversionAmount: Double,
  fromCurr: CurrencyUnit,
  config: ForexConfig,
  client: ForexClient[F]
) {

  /**
   * Continue building the  target currency to the desired one
   * @param toCurr Target currency
   * @return ForexLookupWhen object which is final part of the fluent interface
   */
  def to(toCurr: CurrencyUnit)(implicit F: Sync[F]): ForexLookupWhen[F] =
    ForexLookupWhen(conversionAmount, fromCurr, toCurr, config, client)
}

/**
 * ForexLookupWhen is the final part of the fluent interface,
 * methods in this class are the final stage of currency lookup/conversion
 * @param conversionAmount The amount of money to be converted, it is set to 1 for lookup operation
 * @param fromCurr The source currency
 * @param toCurr The target currency
 * @param config Forex config
 * @param client Passed down client that does actual work
 */
case class ForexLookupWhen[F[_]: Sync](
  conversionAmount: Double,
  fromCurr: CurrencyUnit,
  toCurr: CurrencyUnit,
  config: ForexConfig,
  client: ForexClient[F]
) {
  // convert `conversionAmt` into BigDecimal representation for its later usage in BigMoney
  val conversionAmt = new BigDecimal(conversionAmount)

  /**
   * Performs live currency lookup/conversion
   * @return Money representation in target currency or OerResponseError object if API request
   * failed
   */
  def now: F[Either[OerResponseError, Money]] = {
    val fromF = EitherT(client.getLiveCurrencyValue(fromCurr))
    val toF   = EitherT(client.getLiveCurrencyValue(toCurr))

    fromF
      .product(toF)
      .flatMapF {
        case (fromRate, toRate) =>
          val fromCurrIsBaseCurr = fromCurr == config.baseCurrency
          val rate               = Forex.getForexRate(fromCurrIsBaseCurr, fromRate, toRate)
          // Note that if `fromCurr` is not the same as the base currency,
          // then we need to add the <fromCurr, toCurr> pair into the cache in particular,
          // because only <baseCurrency, toCurr> were added earlier
          client.nowishCache
            .filter(_ => fromCurr != config.baseCurrency)
            .traverse(cache => Sync[F].delay(ZonedDateTime.now).map(dateTime => (cache, dateTime)))
            .flatMap(_.traverse { case (cache, timeStamp) => cache.put((fromCurr, toCurr), (timeStamp, rate)) })
            .map(_ => returnMoneyOrJodaError(rate))
      }
      .value
  }

  /**
   * Performs near-live currency lookup/conversion.
   * A cached version of the live exchange rate is used
   * if a cache exists and the timestamp of that exchange rate is less than or equal to "nowishSecs" old.
   * Otherwise a new lookup is performed.
   * @return Money representation in target currency or OerResponseError object if API request
   * failed
   */
  def nowish: F[Either[OerResponseError, Money]] =
    OptionT
      .fromOption[F](client.nowishCache)
      .flatMap(_ => lookupNowishCache(fromCurr, toCurr))
      .withFilter {
        case (time, _) =>
          val nowishTime = ZonedDateTime.now.minusSeconds(config.nowishSecs.toLong)
          nowishTime.isBefore(time) || nowishTime.equals(time)
      }
      .map { case (_, rate) => returnMoneyOrJodaError(rate) }
      .getOrElseF(now)

  private def lookupNowishCache(
    fromCurr: CurrencyUnit,
    toCurr: CurrencyUnit
  ): OptionT[F, NowishCacheValue] = {
    val oneWay = OptionT(client.nowishCache.flatTraverse(cache => cache.get((fromCurr, toCurr))))
    val otherWay = OptionT(client.nowishCache.flatTraverse(cache => cache.get((toCurr, fromCurr))))
      .map { case (time, rate) => (time, inverseRate(rate)) }

    oneWay.orElse(otherWay)
  }

  /**
   * Gets the latest end-of-day rate prior to the event or post to the event
   * @return Money representation in target currency or OerResponseError object if API request
   * failed
   */
  def at(tradeDate: ZonedDateTime): F[Either[OerResponseError, Money]] = {
    val latestEod = if (config.getNearestDay == EodRoundUp) {
      tradeDate.truncatedTo(ChronoUnit.DAYS).plusDays(1)
    } else {
      tradeDate.truncatedTo(ChronoUnit.DAYS)
    }
    eod(latestEod)
  }

  /**
   * Gets the end-of-day rate for the specified date
   * @return Money representation in target currency or OerResponseError object if API request
   * failed
   */
  def eod(eodDate: ZonedDateTime): F[Either[OerResponseError, Money]] =
    OptionT
      .fromOption[F](client.eodCache)
      .flatMap(_ => lookupEodCache(fromCurr, toCurr, eodDate))
      .map(rate => returnMoneyOrJodaError(rate))
      .getOrElseF(getHistoricalRate(eodDate))

  private def lookupEodCache(
    fromCurr: CurrencyUnit,
    toCurr: CurrencyUnit,
    eodDate: ZonedDateTime
  ): OptionT[F, BigDecimal] = {
    val oneWay = OptionT(client.eodCache.flatTraverse(cache => cache.get((fromCurr, toCurr, eodDate))))
    val otherWay = OptionT(client.eodCache.flatTraverse(cache => cache.get((toCurr, fromCurr, eodDate))))
      .map(inverseRate)

    oneWay.orElse(otherWay)
  }

  private def inverseRate(rate: BigDecimal): BigDecimal =
    new BigDecimal(1).divide(rate, Forex.commonScale, RoundingMode.HALF_EVEN)

  /**
   * Helper method to get the historical forex rate between two currencies on a given date,
   * @return Money in target currency representation or error message if the date given is invalid
   */
  private def getHistoricalRate(date: ZonedDateTime): F[Either[OerResponseError, Money]] = {
    val fromF = EitherT(client.getHistoricalCurrencyValue(fromCurr, date))
    val toF   = EitherT(client.getHistoricalCurrencyValue(toCurr, date))

    fromF
      .product(toF)
      .flatMapF {
        case (fromRate, toRate) =>
          // API request succeeds
          val fromCurrIsBaseCurr = fromCurr == config.baseCurrency
          val rate               = Forex.getForexRate(fromCurrIsBaseCurr, fromRate, toRate)
          // Note that if `fromCurr` is not the same as the base currency,
          // then we need to add the <fromCurr, toCurr> pair into the cache in particular,
          // because only <baseCurrency, toCurr> were added earlier
          client.eodCache
            .filter(_ => fromCurr != config.baseCurrency)
            .traverse_(cache => cache.put((fromCurr, toCurr, date), rate))
            .map(_ => returnMoneyOrJodaError(rate))
      }
      .value
  }

  /**
   * This method is called when the forex rate has been found in the API
   * @param rate The forex rate between source and target currency
   * @return Money representation in target currency if both currencies are supported by Joda Money
   * or OerResponseError with an error message containing the forex rate between the two
   * currencies
   * if either of the currency is not supported by Joda Money
   */
  private def returnMoneyOrJodaError(rate: BigDecimal): Either[OerResponseError, Money] = {
    val moneyInSourceCurrency = BigMoney.of(fromCurr, conversionAmt)

    val moneyTry =
      if (fromCurr == toCurr)
        Try(moneyInSourceCurrency.toMoney(RoundingMode.HALF_EVEN))
      else
        Try(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))

    moneyTry match {
      case Success(m) => Right(m)
      case Failure(e) => Left(OerResponseError(e.getMessage, OtherErrors))
    }
  }
}

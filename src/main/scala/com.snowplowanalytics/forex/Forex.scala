/*
 * Copyright (c) 2013-2017 Snowplow Analytics Ltd. All rights reserved.
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

// Java
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.math.{BigDecimal, RoundingMode}

import com.snowplowanalytics.lrumap.LruMap

// cats
import cats.effect.Sync
import cats.implicits._
import cats.data.{EitherT, OptionT}

// Joda
import org.joda.money._

// OerClient
import com.snowplowanalytics.forex.oerclient._

/**
 * Companion object to get Forex object
 */
object Forex {

  /**
   * Fields for calculating currency rates conversions.
   * Usually the currency rate has 6 decimal places
   */
  val commonScale = 6

  /** Helper method to get the ratio of from:to in BigDecimal type */
  def getForexRate(fromCurrIsBaseCurr: Boolean, baseOverFrom: BigDecimal, baseOverTo: BigDecimal): BigDecimal =
    if (!fromCurrIsBaseCurr) {
      val fromOverBase = new BigDecimal(1).divide(baseOverFrom, Forex.commonScale, RoundingMode.HALF_EVEN)
      fromOverBase.multiply(baseOverTo)
    } else {
      baseOverTo
    }

  /**
   * Getter for Forex object
   */
  def getForex[F[_]: Sync](config: ForexConfig, clientConfig: ForexClientConfig): Forex[F] =
    new Forex[F](config, clientConfig)

  /**
   * Getter for Forex object with user defined caches
   */
  def getForex[F[_]: Sync](config: ForexConfig,
                           clientConfig: ForexClientConfig,
                           nowish: Option[NowishCache[F]],
                           eod: Option[EodCache[F]]): Forex[F] =
    new Forex[F](config, clientConfig, nowishCache = nowish, eodCache = eod)
}

/**
 * Starts building the fluent interface for currency look-up and conversion,
 * Forex class has methods rate and convert, which set the source currency to
 * the currency we are interested in.
 * They return ForexLookupTo object which is then passed to methods
 * in ForexLookupTo class in the fluent interface.
 * @param config - a configurator for Forex object
 * @param clientConfig - a configurator for Forex Client object
 * @param nowishCache - user defined nowishCache
 * @param eodCache - user defined eodCache
 */
case class Forex[F[_]: Sync](config: ForexConfig,
                             clientConfig: ForexClientConfig,
                             nowishCache: Option[NowishCache[F]] = None,
                             eodCache: Option[EodCache[F]]       = None) {

  // For now, we are hard-wired to the OER Client library
  val client: ForexClient[F] = ForexClient.getClient[F](config, clientConfig, nowishCache, eodCache)

  def rate: ForexLookupTo[F] =
    ForexLookupTo(1, config.baseCurrency, this)

  /**
   * Starts building a fluent interface,
   * performs currency look up from *source* currency.
   * (The target currency will be supplied
   * to the ForexLookupTo later).
   * If not specified, it is set to base currency by default.
   * @param currency(optional) - source currency
   * @return ForexLookupTo object which is the part of the fluent interface
   */
  def rate(currency: String): ForexLookupTo[F] =
    ForexLookupTo(1, currency, this)

  /*
   * Wrapper for rate(currency: String)
   */
  def rate(currency: CurrencyUnit): ForexLookupTo[F] =
    rate(currency.getCode)

  /**
   * Starts building a currency conversion from
   * the supplied currency, for the supplied
   * amount.
   * @param amount - The amount of currency to be converted
   * @param currency - The *source* currency(optional).
   * (The target currency will be supplied
   * to the ForexLookupTo later). If not specified,
   * it is set to be base currency by default
   * @return a ForexLookupTo, part of the
   * currency conversion fluent interface.
   */
  def convert(amount: Double): ForexLookupTo[F] =
    ForexLookupTo(amount, config.baseCurrency, this)

  def convert(amount: Double, currency: CurrencyUnit): ForexLookupTo[F] =
    convert(amount, currency.getCode)
  // Wrapper method for convert(Int, CurrencyUnit)
  def convert(amount: Double, currency: String): ForexLookupTo[F] =
    ForexLookupTo(amount, currency, this)
}

/**
 * ForexLookupTo is the second part of the fluent interface,
 * which passes parameters taken from Forex class to the next stage in the fluent interface
 * and sets the target currency for the lookup/conversion.
 * Method in this class returns ForexLookupWhen object which will be passed to the next stage
 * in the fluent interface.
 * @param fx - Forex object which was configured ealier
 * @param conversionAmount - the amount of money to be converted,
 * it is set to 1 unit for look up operation.
 * @param fromCurr - the source currency
 */
case class ForexLookupTo[F[_]: Sync](conversionAmount: Double, fromCurr: String, fx: Forex[F]) {

  /**
   * Continue building the  target currency to the desired one
   * @param currency - Target currency
   * @return ForexLookupWhen object which is final part of the fluent interface
   */
  def to(toCurr: String): ForexLookupWhen[F] =
    ForexLookupWhen(conversionAmount, fromCurr, toCurr, fx)

  // Wrapper for to(toCurr: String)
  def to(toCurr: CurrencyUnit): ForexLookupWhen[F] =
    to(toCurr.getCode)
}

/**
 * ForexLookupWhen is the final part of the fluent interface,
 * methods in this class are the final stage of currency lookup/conversion
 * @param conversionAmount - The amount of money to be converted, it is set to 1 for lookup operation
 * @param fromCurr - The source currency
 * @param toCurr   - The target currency
 * @param fx       - Forex object
 */
case class ForexLookupWhen[F[_]: Sync](conversionAmount: Double, fromCurr: String, toCurr: String, fx: Forex[F]) {
  // convert `conversionAmt` into BigDecimal representation for its later usage in BigMoney
  val conversionAmt = new BigDecimal(conversionAmount)
  // convert `fromCurr` and `toCurr` in string representations to CurrencyUnit representations
  val fromCurrencyUnit = convertToCurrencyUnit(fromCurr)
  val toCurrencyUnit   = convertToCurrencyUnit(toCurr)

  /**
   * Performs live currency lookup/conversion, no caching available
   * @return Money representation in target currency or OerResponseError object if API request failed
   */
  def now: F[Either[OerResponseError, Money]] = {
    val timeStamp = ZonedDateTime.now
    val fromF     = EitherT(fx.client.getLiveCurrencyValue(fromCurr))
    val toF       = EitherT(fx.client.getLiveCurrencyValue(toCurr))

    fromF
      .product(toF)
      .flatMapF {
        case (fromRate, toRate) =>
          val fromCurrIsBaseCurr = fromCurr == fx.config.baseCurrency
          val rate               = Forex.getForexRate(fromCurrIsBaseCurr, fromRate, toRate)
          // Note that if `fromCurr` is not the same as the base currency,
          // then we need to add the <fromCurr, toCurr> pair into the cache in particular,
          // because only <baseCurrency, toCurr> were added earlier
          fx.client.nowishCache
            .filter(_ => fromCurr != fx.config.baseCurrency)
            .traverse(cache => cache.put((fromCurr, toCurr), (timeStamp, rate)))
            .map(_ => returnMoneyOrJodaError(rate))
      }
      .leftMap(error => returnApiError(error))
      .value

  }

  /**
   * Performs near-live currency lookup/conversion.
   * A cached version of the live exchange rate is used
   * if a cache exists and the timestamp of that exchange rate is less than or equal to "nowishSecs" old.
   * Otherwise a new lookup is performed.
   * @return Money representation in target currency or OerResponseError object if API request failed
   */
  def nowish: F[Either[OerResponseError, Money]] =
    OptionT
      .fromOption[F](fx.client.nowishCache)
      .flatMap(cache => lookupNowishCache(fromCurr, toCurr))
      .withFilter {
        case (time, _) =>
          val nowishTime = ZonedDateTime.now.minusSeconds(fx.config.nowishSecs.toLong)
          nowishTime.isBefore(time) || nowishTime.equals(time)
      }
      .map { case (_, rate) => returnMoneyOrJodaError(rate) }
      .getOrElseF(now)

  private def lookupNowishCache(fromCurr: String, toCurr: String): OptionT[F, NowishCacheValue] = {
    val oneWay = OptionT(fx.client.nowishCache.flatTraverse(cache => cache.get((fromCurr, toCurr))))
    val otherWay = OptionT(fx.client.nowishCache.flatTraverse(cache => cache.get((toCurr, fromCurr))))
      .map { case (time, rate) => (time, inverseRate(rate)) }

    oneWay.orElse(otherWay)
  }

  /**
   * Gets the latest end-of-day rate prior to the event or post to the event
   * @return Money representation in target currency or OerResponseError object if API request failed
   */
  def at(tradeDate: ZonedDateTime): F[Either[OerResponseError, Money]] = {
    val latestEod = if (fx.config.getNearestDay == EodRoundUp) {
      tradeDate.truncatedTo(ChronoUnit.DAYS).plusDays(1)
    } else {
      tradeDate.truncatedTo(ChronoUnit.DAYS)
    }
    eod(latestEod)
  }

  /**
   * Gets the end-of-day rate for the specified date
   * @return Money representation in target currency or OerResponseError object if API request failed
   */
  def eod(eodDate: ZonedDateTime): F[Either[OerResponseError, Money]] =
    OptionT
      .fromOption[F](fx.client.eodCache)
      .flatMap(cache => lookupEodCache(fromCurr, toCurr, eodDate))
      .map(rate => returnMoneyOrJodaError(rate))
      .getOrElseF(getHistoricalRate(eodDate))

  private def lookupEodCache(fromCurr: String, toCurr: String, eodDate: ZonedDateTime): OptionT[F, BigDecimal] = {
    val oneWay = OptionT(fx.client.eodCache.flatTraverse(cache => cache.get((fromCurr, toCurr, eodDate))))
    val otherWay = OptionT(fx.client.eodCache.flatTraverse(cache => cache.get((toCurr, fromCurr, eodDate))))
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
    val fromF = EitherT(fx.client.getHistoricalCurrencyValue(fromCurr, date))
    val toF   = EitherT(fx.client.getHistoricalCurrencyValue(toCurr, date))

    fromF
      .product(toF)
      .flatMapF {
        case (fromRate, toRate) =>
          // API request succeeds
          val fromCurrIsBaseCurr = fromCurr == fx.config.baseCurrency
          val rate               = Forex.getForexRate(fromCurrIsBaseCurr, fromRate, toRate)
          // Note that if `fromCurr` is not the same as the base currency,
          // then we need to add the <fromCurr, toCurr> pair into the cache in particular,
          // because only <baseCurrency, toCurr> were added earlier
          fx.client.eodCache
            .filter(_ => fromCurr != fx.config.baseCurrency)
            .traverse_(cache => cache.put((fromCurr, toCurr, date), rate))
            .map(_ => returnMoneyOrJodaError(rate))
      }
      .leftMap(error => returnApiError(error))
      .value
  }

  /**
   * Helper method to convert a currency type in String representation to CurrencyUnit representation
   * @param currencyInStringRepresentation - the currency to be converted
   * @return CurrencyUnit representation of the currency, or OerResponseError if Joda money does not support the currency
   */
  private def convertToCurrencyUnit(currencyInStringRepresentation: String): Either[OerResponseError, CurrencyUnit] =
    try {
      Right(CurrencyUnit.of(currencyInStringRepresentation))
    } catch {
      case e: IllegalCurrencyException =>
        val errMessage = "Currency [" + fromCurr + "] is not supported by Joda money "
        Left(OerResponseError(errMessage, IllegalCurrency))
    }

  /**
   * This method is called when the forex rate has been found in the API
   * @param rate - The forex rate between source and target currency
   * @return Money representation in target currency if both currencies are supported by Joda Money
   * or OerResponseError with an error message containing the forex rate between the two currencies
   * if either of the currency is not supported by Joda Money
   */
  private def returnMoneyOrJodaError(rate: BigDecimal): Either[OerResponseError, Money] =
    fromCurrencyUnit
      .product(toCurrencyUnit)
      .map {
        case (fromCurrency, toCurrency) =>
          val moneyInSourceCurrency = BigMoney.of(fromCurrency, conversionAmt)

          if (fromCurrency == toCurrency)
            moneyInSourceCurrency.toMoney(RoundingMode.HALF_EVEN)
          else
            moneyInSourceCurrency.convertedTo(toCurrency, rate).toMoney(RoundingMode.HALF_EVEN)
      }
      .leftMap { _ =>
        var errMessage = "The exchange rate of [" + fromCurr + "]:[" + toCurr + "] " +
          "is " + rate + ". However, "
        errMessage += fromCurrencyUnit.fold(_.errorMessage, _ => "")
        errMessage += toCurrencyUnit.fold(_.errorMessage, _   => "")
        OerResponseError(errMessage, IllegalCurrency)
      }

  /**
   * This method is called if API requests fail
   * @param errObject - The OerResponseError object which contains the failure information returned from API
   * @return OerResponseError object which states the failure information
   * and also illegal currency info if there is any illegal currency
   */
  private def returnApiError(errObject: OerResponseError): OerResponseError = {
    var errMsg = ""
    errMsg += fromCurrencyUnit.fold(_.errorMessage, _ => "")
    errMsg += toCurrencyUnit.fold(_.errorMessage, _   => "")
    errMsg += errObject.errorMessage
    OerResponseError(errMsg, errObject.errorType)
  }
}

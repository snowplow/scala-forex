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

// cats
import cats.effect.Sync
import cats.{Applicative, Functor}
import cats.syntax.apply._
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.flatMap._

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
                           nowish: MaybeNowishCache,
                           eod: MaybeEodCache): Forex[F] =
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
                             nowishCache: MaybeNowishCache = None,
                             eodCache: MaybeEodCache       = None) {

  // For now, we are hard-wired to the OER Client library
  val client = ForexClient.getClient[F](config, clientConfig, nowish = nowishCache, eod = eodCache)

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
    val fromF     = fx.client.getLiveCurrencyValue(fromCurr)
    val toF       = fx.client.getLiveCurrencyValue(toCurr)

    (fromF, toF).mapN { (from, to) =>
      if (from.isRight && to.isRight) {
        // API request succeeds
        val baseOverFrom       = from.right.get
        val baseOverTo         = to.right.get
        val fromCurrIsBaseCurr = fromCurr == fx.config.baseCurrency
        val rate               = Forex.getForexRate(fromCurrIsBaseCurr, baseOverFrom, baseOverTo)
        // Note that if `fromCurr` is not the same as the base currency,
        // then we need to add the <fromCurr, toCurr> pair into the cache in particular,
        // because only <baseCurrency, toCurr> were added earlier
        val action = if (fx.client.caches.nowish.isDefined && fromCurr != fx.config.baseCurrency) {
          val Some(cache) = fx.client.caches.nowish
          Sync[F].delay(cache.put((fromCurr, toCurr), (timeStamp, rate)))
        } else Sync[F].unit

        action.map(_ => returnMoneyOrJodaError(rate))
      } else {
        // API request fails
        returnApiError(from.left.get).pure[F]
      }
    }.flatten

  }

  /**
   * Performs near-live currency lookup/conversion.
   * A cached version of the live exchange rate is used
   * if a cache exists and the timestamp of that exchange rate is less than or equal to "nowishSecs" old.
   * Otherwise a new lookup is performed.
   * @return Money representation in target currency or OerResponseError object if API request failed
   */
  def nowish: F[Either[OerResponseError, Money]] =
    fx.client.caches.nowish match {
      case Some(cache) => {
        val nowishTime = ZonedDateTime.now.minusSeconds(fx.config.nowishSecs)
        cache.get((fromCurr, toCurr)) match {
          // from:to found in LRU cache
          case Some(tpl) =>
            val (timeStamp, exchangeRate) = tpl
            if (nowishTime.isBefore(timeStamp) || nowishTime.equals(timeStamp)) {
              // the timestamp in the cache is within the allowed range
              returnMoneyOrJodaError(exchangeRate).pure[F]
            } else {
              now
            }
          // from:to not found in LRU
          case None =>
            cache.get((toCurr, fromCurr)) match {
              // to:from found in LRU
              case Some(tpl) => {
                val (time, rate) = tpl
                returnMoneyOrJodaError(new BigDecimal(1).divide(rate, Forex.commonScale, RoundingMode.HALF_EVEN))
                  .pure[F]
              }
              // Neither direction found in LRU
              case None => {
                now
              }
            }
        }
      }
      // If cache is disabled, nowish lookup will perform exactly the same as now()
      case None => now
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
    fx.client.caches.eod match {
      case Some(cache) => {
        cache.get((fromCurr, toCurr, eodDate)) match {
          // from->to is found in the cache
          case Some(rate) =>
            returnMoneyOrJodaError(rate).pure[F]
          // from->to not found in the cache
          case None =>
            cache.get((toCurr, fromCurr, eodDate)) match {
              // to->from found in the cache
              case Some(exchangeRate) =>
                returnMoneyOrJodaError(
                  new BigDecimal(1).divide(exchangeRate, Forex.commonScale, RoundingMode.HALF_EVEN)).pure[F]
              // neither from->to nor to->from found in the cache
              case None =>
                getHistoricalRate(eodDate)
            }
        }
      }
      case None => getHistoricalRate(eodDate)
    }

  /**
   * Helper method to get the historical forex rate between two currencies on a given date,
   * @return Money in target currency representation or error message if the date given is invalid
   */
  private def getHistoricalRate(date: ZonedDateTime): F[Either[OerResponseError, Money]] = {
    val fromF = fx.client.getHistoricalCurrencyValue(fromCurr, date)
    val toF   = fx.client.getHistoricalCurrencyValue(toCurr, date)

    (fromF, toF).mapN { (from, to) =>
      if (from.isRight && to.isRight) {
        // API request succeeds
        val baseOverFrom       = from.right.get
        val baseOverTo         = to.right.get
        val fromCurrIsBaseCurr = fromCurr == fx.config.baseCurrency
        val rate               = Forex.getForexRate(fromCurrIsBaseCurr, baseOverFrom, baseOverTo)
        // Note that if `fromCurr` is not the same as the base currency,
        // then we need to add the <fromCurr, toCurr> pair into the cache in particular,
        // because only <baseCurrency, toCurr> were added earlier
        val action = if (fx.client.caches.eod.isDefined && fromCurr != fx.config.baseCurrency) {
          val Some(cache) = fx.client.caches.eod
          Sync[F].delay(cache.put((fromCurr, toCurr, date), rate))
        } else Sync[F].unit

        action.map(_ => returnMoneyOrJodaError(rate))
      } else {
        // API request fails
        returnApiError(from.left.get).pure[F]
      }
    }.flatten

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
    if (fromCurrencyUnit.isRight && toCurrencyUnit.isRight) {
      // Money in a given amount
      val moneyInSourceCurrency = BigMoney.of(fromCurrencyUnit.right.get, conversionAmt)
      // prevent weird JodaTime exception when converting equal currencies
      if (fromCurrencyUnit.right.get == toCurrencyUnit.right.get) {
        Right(moneyInSourceCurrency.toMoney(RoundingMode.HALF_EVEN))
      } else {
        Right(moneyInSourceCurrency.convertedTo(toCurrencyUnit.right.get, rate).toMoney(RoundingMode.HALF_EVEN))
      }
    } else {
      var errMessage = "The exchange rate of [" + fromCurr + "]:[" + toCurr + "] " +
        "is " + rate + ". However, "
      if (fromCurrencyUnit.isLeft) {
        errMessage += fromCurrencyUnit.left.get.errorMessage
      }
      if (toCurrencyUnit.isLeft) {
        errMessage += toCurrencyUnit.left.get.errorMessage
      }
      Left(OerResponseError(errMessage, IllegalCurrency))
    }

  /**
   * This method is called if API requests fail
   * @param errObject - The OerResponseError object which contains the failure information returned from API
   * @return OerResponseError object which states the failure information
   * and also illegal currency info if there is any illegal currency
   */
  private def returnApiError(errObject: OerResponseError): Either[OerResponseError, Money] = {
    var errMsg = ""
    if (fromCurrencyUnit.isLeft) {
      errMsg += fromCurrencyUnit.left.get.errorMessage
    }
    if (toCurrencyUnit.isLeft) {
      errMsg += toCurrencyUnit.left.get.errorMessage
    }
    errMsg += errObject.errorMessage
    Left(OerResponseError(errMsg, errObject.errorType))
  }
}

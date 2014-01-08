/*
 * Copyright (c) 2013-2014 Snowplow Analytics Ltd. All rights reserved.
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
import java.math.BigDecimal
import java.math.RoundingMode
// Joda 
import org.joda.time._
import org.joda.money._
// Scala
import scala.collection.JavaConversions._
// OerClient
import com.snowplowanalytics.forex.oerclient._


/**
* Companion object to get Forex object
* Either pass in spied caches to spy on the Forex object or get the normal version of Forex object
*/
object Forex {
  def getSpiedForex(config: ForexConfig, oerconfig: OerClientConfig, 
                      spiedNowishCache: NowishCacheType, spiedEodCache: EodCacheType): Forex = {
    new Forex(config, oerconfig, nowishCache = spiedNowishCache, eodCache = spiedEodCache)
  }

  def getForex(config: ForexConfig, oerconfig: OerClientConfig): Forex = {
    new Forex(config, oerconfig)
  }
}

/**
 * Starts building the fluent interface for currency look-up and conversion,
 * Forex class has methods rate and convert which return ForexLookupTo object
 * which will be passed to the method to in class ForexLookupTo
 * @pvalue config - a configurator for Forex object
 * @pvalue oerConfig - a configurator for OER Client object 
 * @pvalue nowishCache - spy for nowishCache
 * @pvalue eodCache - spy for eodCache
 */
case class Forex(config: ForexConfig, oerConfig: OerClientConfig, 
                  nowishCache: NowishCacheType = None, eodCache: EodCacheType  = None) {
  val client = ForexClient.getOerClient(config, oerConfig, nowish = nowishCache, eod = eodCache)
  // preserve 10 digits after decimal point of a number when performing division 
  val maxScale         = 10 
  // usually the number of digits of a currency value has only 6 digits 
  val commonScale      = 6 

  /**
  * starts building a fluent interafce, 
  * performs currency look up from base currency 
  * @returns ForexLookupTo object which is the part of the fluent interface
  */
  def rate: ForexLookupTo = {
    ForexLookupTo(1, config.baseCurrency, this)
  }

  /**
  * starts building a fluent interface, 
  * performs currency look up from the desired currency
  * @param currency - CurrencyUnit type
  * @returns ForexLookupTo object which is the part of the fluent interface
  */
  def rate(currency: CurrencyUnit): ForexLookupTo = {
    rate(currency.getCode)
  }

  // wrapper method for rate(CurrencyUnit)
  // if the string is invalid, getInstance will throw IllegalCurrencyException
  def rate(currency: String): ForexLookupTo = {
    ForexLookupTo(1, currency, this)
  }

  /**
   * Starts building a currency conversion from
   * the supplied currency, for the supplied
   * amount. Returns a ForexLookupTo to finish
   * the conversion.
   *
   * @param amount - The amount of currency to
   * convert
   * @param currency - The *source* currency(optional).
   * (The target currency will be supplied
   * to the ForexLookupTo later). If not specified, 
   * it is set to be base currency by default
   * @returns a ForexLookupTo, part of the
   * currency conversion fluent interface.
   */
  def convert(amount: Int): ForexLookupTo = {
    ForexLookupTo(amount, config.baseCurrency, this)
  }
  
  def convert(amount: Int, currency: CurrencyUnit): ForexLookupTo = {
    convert(amount, currency.getCode)
  }
  // wrapper method for convert(Int, CurrencyUnit)
  def convert(amount: Int, currency: String): ForexLookupTo = {     
    ForexLookupTo(amount, currency, this)
  }
}


/**
 * ForexLookupTo is part of the fluent interface,
 * it only has method to which returns ForexLookupWhen object 
 * which will then be passed to ForexLookupWhen class
 * @pvalue fx - Forex object which was configured ealier 
 * @pvalue conversionAmount - the amount of money to be converted, it is set to 1 unit for look up operation, 
 * the argument will be passed to ForexLookUpWhen class 
 * @pvalue fromCurr - the source currency, the argument will be passed to ForexLookUpWhen class  
 */
case class ForexLookupTo(conversionAmount: Int, fromCurr: String, fx: Forex) {
  /**
   * this method sets the target currency to the desired one
   * @param currency - Target currency
   * @return ForexLookupWhen object which is part of the fluent interface
   */
  def to(toCurr: CurrencyUnit): ForexLookupWhen = {
    to(toCurr.getCode)
  }
  // wrapper method for to(CurrencyUnit)
  def to(toCurr: String): ForexLookupWhen = {
    ForexLookupWhen(conversionAmount, fromCurr, toCurr, fx)
  }
}

/**
* ForexLookupWhen is the end of the fluent interface,
* methods in this class are the final stage of currency lookup and conversion
* @pvalue fx - Forex object 
* @pvalue conversionAmount - The amount of money to be converted, it is set to 1 unit for look up operation
* @pvalue fromCurr - The source currency
* @pvalue toCurr   - The target currency
*/
case class ForexLookupWhen(conversionAmount: Int, fromCurr: String, toCurr: String, fx: Forex) {
  // convert conversionAmt units of money 
  val conversionAmt = new BigDecimal(conversionAmount)                      
  // convert from currency and to currency in string representation to CurrencyUnit representation                
  val fromCurrencyUnit = convertToCurrencyUnit(fromCurr) 
  val toCurrencyUnit   = convertToCurrencyUnit(toCurr)

  /**
  * Perform live currency look up or conversion, no caching needed
  * @returns Money representation in target currency or OerResponseError object if API request failed
  */
  def now: Either[OerResponseError, Money] =  {
    val timeStamp = DateTime.now
    val from = fx.client.getLiveCurrencyValue(fromCurr)
    val to   = fx.client.getLiveCurrencyValue(toCurr)
    if (from.isRight && to.isRight) {
    // API request succeeds
      val baseOverFrom = from.right.get
      val baseOverTo = to.right.get
      val rate = getForexRate(baseOverFrom, baseOverTo)
      // if from currency is not base currency then we need to add the fromCurrency->toCurrency pair into cache 
      // because getLiveCurrencyValue method only adds baseCurrency->toCurrency into cache
      if (!fx.client.nowishCache.isEmpty && fromCurr != fx.config.baseCurrency) {
        val Some(cache) = fx.client.nowishCache
        cache.put((fromCurr,toCurr), (timeStamp, rate))
      }
      returnMoneyOrJodaError(rate)
    } else {
    // API request fails
      returnApiError(from.left.get)
    }
  }
  
  
  /**
  * a cached version of the live exchange rate is used if cache exists, 
  * if the timestamp of that exchange rate is less than 
  * or equal to "nowishSecs" old. Otherwise a new lookup is performed.
  */
  def nowish: Either[OerResponseError, Money] = {
    fx.client.nowishCache match {
      case Some(cache) => {
          val nowishTime = DateTime.now.minusSeconds(fx.config.nowishSecs)
          cache.get((fromCurr, toCurr)) match {
            // from:to found in LRU cache
            case Some(tpl) => {
              val (timeStamp, exchangeRate) = tpl
              if (nowishTime.isBefore(timeStamp) || nowishTime.equals(timeStamp)) {
                // the timestamp in the cache is within the allowed range  
                returnMoneyOrJodaError(exchangeRate)
              } else {
                now
              }
            }
            // from:to not found in LRU
            case None => {
              cache.get((toCurr, fromCurr)) match {
                // to:from found in LRU
                case Some(tpl) => { 
                  val (time, rate) = tpl
                  returnMoneyOrJodaError(new BigDecimal(1).divide(rate, fx.commonScale, RoundingMode.HALF_EVEN))
                }
                // Neither direction found in LRU
                case None => {
                  now
                }
              }
            }
          }
        }
      // if cache is disabled, nowish lookup will perform exactly the same as now()
      case None => now
    }
  }

  /**
  * gets the latest end-of-day rate prior to the datetime by default or
  * on the closer day if the getNearestDay flag is true
  */
  def at(tradeDate: DateTime): Either[OerResponseError, Money] = {
    val latestEod = if (fx.config.getNearestDay == EodRoundUp) {
      tradeDate.withTimeAtStartOfDay.plusDays(1)
    } else {
      tradeDate.withTimeAtStartOfDay
    }
    eod(latestEod)   
  }

  /**
  * gets the end-of-day rate for the specified day
  */
  def eod(eodDate: DateTime): Either[OerResponseError, Money] = { 
    fx.client.eodCache match {
      case Some(cache) => {
        cache.get((fromCurr, toCurr, eodDate)) match {
          // from->to is found in the cache 
          case Some(rate) => 
            returnMoneyOrJodaError(rate)
          // from->to not found in the cache
          case None => 
            cache.get((toCurr, fromCurr, eodDate)) match {                  
              // to->from found in the cache
              case Some(exchangeRate) =>                                              
                returnMoneyOrJodaError(new BigDecimal(1).divide(exchangeRate, fx.commonScale, RoundingMode.HALF_EVEN))
              // neither from->to nor to->from found in the cache
              case None =>
                getHistoricalRate(eodDate)         
            }
        }
      }
      case None =>  getHistoricalRate(eodDate)
    }
  }

  /**
  * Gets historical forex rate between two currencies on a given date,
  * baseOverFrom and baseOverTo have the same date so either they both return Left or both return Right 
  * @returns Money in target currency representation or error message if the date given is invalid 
  */
  private def getHistoricalRate(date: DateTime): Either[OerResponseError, Money] = { 
    val from = fx.client.getHistoricalCurrencyValue(fromCurr, date)
    val to   = fx.client.getHistoricalCurrencyValue(toCurr, date)
    if (from.isRight && to.isRight) {
      val baseOverFrom = from.right.get
      val baseOverTo = to.right.get
      val rate = getForexRate(baseOverFrom, baseOverTo)
      // if from currency is not base currency then we need to add the fromCurrency->toCurrency pair into cache 
      // because getHistoricalCurrencyValue method only adds baseCurrency->toCurrency into cache
      if (!fx.client.eodCache.isEmpty && fromCurr != fx.config.baseCurrency) {
        val Some(cache) = fx.client.eodCache
        cache.put((fromCurr,toCurr, date), rate)
      }
      returnMoneyOrJodaError(rate)
    } else {
      returnApiError(from.left.get)
    }
  }

  /** 
  * Gets the forex rate between source currency and target currency
  * @returns ratio of from:to as BigDecimal
  */
  private def getForexRate(baseOverFrom: BigDecimal, baseOverTo: BigDecimal): BigDecimal = {
    if (fromCurr != CurrencyUnit.getInstance(fx.config.baseCurrency)) {
      val fromOverBase = new BigDecimal(1).divide(baseOverFrom, fx.commonScale, RoundingMode.HALF_EVEN)
      fromOverBase.multiply(baseOverTo)
    } else {
      baseOverTo
    }
  }
  
  /**
  * Converts a currency type in String representation to CurrencyUnit representation 
  * @param currencyInStringRepresentation - the currency to be converted
  * @returns currencyUnit representation of the currency, or OerResponseError if Joda money does not support the currency
  */
  private def convertToCurrencyUnit(currencyInStringRepresentation: String) : Either[OerResponseError, CurrencyUnit] = {
    try {
      Right(CurrencyUnit.getInstance(currencyInStringRepresentation))
    } catch {
      case (e: IllegalCurrencyException) => 
        val errMessage = "Currency [" + fromCurr + "] is not supported by Joda money "
        Left(OerResponseError(errMessage, IllegalCurrency))
    }
  }

  /**
  * This method is called when the forex rate has been found in the API
  * @param rate - The forex rate between source and target currency
  * @returns Money representation in target currency if both currencies are supported by Joda Money
  * or OerResponseError with an error message containing the forex rate between the two currencies 
  * if either of the currency is not supported by Joda Money
  */
  private def returnMoneyOrJodaError(rate: BigDecimal): Either[OerResponseError, Money] = {
    if (fromCurrencyUnit.isRight && toCurrencyUnit.isRight) {
      // Money in a given amount
      val moneyInSourceCurrency = BigMoney.of(fromCurrencyUnit.right.get, conversionAmt) 
      Right(moneyInSourceCurrency.convertedTo(toCurrencyUnit.right.get, rate).toMoney(RoundingMode.HALF_EVEN))
    } else {
      var errMessage = "Currency [" + fromCurr + "] and ["+ toCurr + "] can be found from the API " + 
                       "and the forex rate is [" + fromCurr + "]:[" + toCurr + "] = " + rate + ". However, " 
      if (fromCurrencyUnit.isLeft) {
        errMessage += fromCurrencyUnit.left.get.getMessage
      }
      if (toCurrencyUnit.isLeft) {
        errMessage += toCurrencyUnit.left.get.getMessage
      }
      println(errMessage)
      Left(OerResponseError(errMessage, IllegalCurrency))
    } 
  }

  /**
  * This method is called if API requests fail
  * @param errObject - The OerResponseError object which contains the failure information obtained from API
  * @returns a Left of OerResponseError object which states the failure information 
  * and illegal currency info if there is any illegal currency
  */
  private def returnApiError(errObject : OerResponseError): Either[OerResponseError, Money] = {
    var errMsg = "" 
    if (fromCurrencyUnit.isLeft) {
      errMsg += fromCurrencyUnit.left.get.getMessage
    } 
    if (toCurrencyUnit.isLeft) {
      errMsg += toCurrencyUnit.left.get.getMessage
    }
    errMsg += errObject.getMessage
    println(errMsg)
    Left(OerResponseError(errMsg, OtherErrors))
  }
}


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
import java.net.MalformedURLException
import java.io.FileNotFoundException
import java.io.IOException
// Joda 
import org.joda.time._
import org.joda.money._
// Scala
import scala.collection.JavaConversions._
 
/**
 * Starts building the fluent interface for currency look-up and conversion,
 * Forex class has methods rate and convert which return ForexLookupTo object
 * which will be passed to the method to in class ForexLookupTo
 * @pvalue config - a configurator for Forex object
 */

case class Forex(config: ForexConfig, oerConfig: OerClientConfig) {
  val client = ForexClient.getOerClient(config, oerConfig)
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
    ForexLookupTo(1, currency, this)
  }

  // wrapper method for rate(CurrencyUnit)
  // if the string is invalid, getInstance will throw IllegalCurrencyException
  def rate(currency: String): Either[String,ForexLookupTo] = {
    try {
      val currInstance = CurrencyUnit.getInstance(currency)
      Right(rate(currInstance))
    } catch {
      case (e: IllegalCurrencyException) => Left(e.getMessage)
    }   
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
    ForexLookupTo(amount, currency, this)
  }
  // wrapper method for convert(Int, CurrencyUnit)
  // if the string is invalid, getInstance will throw IllegalCurrencyException
  def convert(amount: Int, currency: String): Either[String, ForexLookupTo] = {      
    try {
      val currInstance = CurrencyUnit.getInstance(currency)
      Right(convert(amount, currInstance))
    } catch {
      case (e: IllegalCurrencyException) => Left(e.getMessage)
    }
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
case class ForexLookupTo(conversionAmount: Int, fromCurr: CurrencyUnit, fx: Forex) {
  /**
   * this method sets the target currency to the desired one
   * @param currency - Target currency
   * @return ForexLookupWhen object which is part of the fluent interface
   */
  def to(toCurr: CurrencyUnit): ForexLookupWhen = {
    ForexLookupWhen(conversionAmount, fromCurr, toCurr, fx)
  }
  // wrapper method for to(CurrencyUnit)
  // if the string is invalid, getInstance will throw IllegalCurrencyException
  def to(toCurr: String): Either[String, ForexLookupWhen] = {
    try {
      val currInstance = CurrencyUnit.getInstance(toCurr)
      Right(to(currInstance))
    } catch {
      case (e: IllegalCurrencyException) => Left(e.getMessage)
    }
  }
}

/**
* ForexLookupWhen is the end of the fluent interface,
* methods in this class are the final stage of currency lookup and conversion
* and return error mesage as string if an exception is caught 
* or Money if an exchange rate is found 
* if MalFormedURLException is caught, then the user must entered wrong app Id, which the API cannot recognise
* @pvalue fx - Forex object 
* @pvalue conversionAmount - The amount of money to be converted, it is set to 1 unit for look up operation
* @pvalue fromCurr - The source currency
* @pvalue toCurr   - The target currency
*/
case class ForexLookupWhen(conversionAmount: Int, fromCurr: CurrencyUnit, toCurr: CurrencyUnit, fx: Forex) {
  val conversionAmt = new BigDecimal(conversionAmount)                      
  // money in a given amount 
  val moneyInSourceCurrency = BigMoney.of(fromCurr, conversionAmt)

  /**
  * perform live currency look up or conversion, no caching needed
  */
  def now: Either[String, Money] =  {
    try {
      val baseOverFrom = fx.client.getCurrencyValue(fromCurr) 
      val baseOverTo = fx.client.getCurrencyValue(toCurr) 
      val rate = getForexRate(baseOverFrom, baseOverTo)
      Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
    } catch {
      case (e: MalformedURLException)    => Left("invalid app Id")
      case (e: IOException)              => Left(e.getMessage)
    }
  }
  
  
  /**
  * a cached version of the live exchange rate is used if cache exists, 
  * if the timestamp of that exchange rate is less than 
  * or equal to "nowishSecs" old. Otherwise a new lookup is performed.
  */
  def nowish: Either[String, Money] = {
    fx.client.nowishCache match {
      case Some(cache) => 
        {
          try {
            val nowishTime = DateTime.now.minusSeconds(fx.config.nowishSecs)
            cache.get((fromCurr, toCurr)) match {
              // from:to found in LRU cache
              case Some(tpl) => {
                val (timeStamp, exchangeRate) = tpl
                if (nowishTime.isBefore(timeStamp) || nowishTime.equals(timeStamp)) {
                   // the timestamp in the cache is within the allowed range 
                  Right(moneyInSourceCurrency.convertedTo(toCurr, exchangeRate).toMoney(RoundingMode.HALF_EVEN))
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
                    val inverseRate = new BigDecimal(1).divide(rate, fx.commonScale, RoundingMode.HALF_EVEN)
                    Right(moneyInSourceCurrency.convertedTo(toCurr, inverseRate).toMoney(RoundingMode.HALF_EVEN))
                  }
                  // Neither direction found in LRU
                  case None => {
                    now
                  }
                }
              }
            }
          } catch {
            case (e: IllegalCurrencyException) => Left(e.getMessage)
            case (e: MalformedURLException)    => Left("invalid URL")
            case (e: IOException)              => Left(e.getMessage)
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
  def at(tradeDate: DateTime): Either[String, Money] = {
    val latestEod = if (fx.config.getNearestDay == EodRoundUp) {
      tradeDate.withTimeAtStartOfDay.plusDays(1)
    } else {
      tradeDate.withTimeAtStartOfDay
    }
    eod(latestEod)   
  }

  /**
  * gets the end-of-date rate for the specified day
  */
  def eod(eodDate: DateTime): Either[String, Money] = { 
    fx.client.eodCache match {
      case Some(cache) => {
        try {
          cache.get((fromCurr, toCurr, eodDate)) match {
            // from->to is found in the cache  
            case Some(rate) => 
              Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))          
            // from->to not found in the cache
            case None => 
              cache.get((toCurr, fromCurr, eodDate)) match {                  
                // to->from found in the cache
                case Some(exchangeRate) =>                                              
                  val rate = new BigDecimal(1).divide(exchangeRate, fx.commonScale, RoundingMode.HALF_EVEN)
                  Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
                // neither from->to nor to->from found in the cache
                case None =>
                  getHistoricalRate(eodDate)         
              }
          }
        } catch {
            case (e: IllegalCurrencyException) => Left(e.getMessage)
            case (e: MalformedURLException) => Left("invalid URL")
            case (e: IOException) =>           Left(e.getMessage)
        }
      }

      case None =>  getHistoricalRate(eodDate)
    }
  }

  /**
  * gets historical forex rate between two currencies on a given date,
  * baseOverFrom and baseOverTo have the same date so either they both return Left or both return Right 
  * @returns Money in target currency representation or error message if the date given is invalid 
  */
  private def getHistoricalRate(date: DateTime): Either[String, Money] = {
    val baseOverFrom = fx.client.getHistoricalCurrencyValue(fromCurr, date)
    val baseOverTo   = fx.client.getHistoricalCurrencyValue(toCurr, date)
    if (baseOverFrom.isRight && baseOverTo.isRight) {
      val rate = getForexRate(baseOverFrom.right.get, baseOverTo.right.get)
      Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
    } else {
      Left(baseOverFrom.left.get)
    }
  }

  /** 
  * gets the forex rate between source currency and target currency
  * @returns ratio of from:to as BigDecimal
  */
  private def getForexRate(baseOverFrom: BigDecimal, baseOverTo: BigDecimal): BigDecimal = {
    if (fromCurr != fx.config.baseCurrency) {
      val fromOverBase = new BigDecimal(1).divide(baseOverFrom, fx.commonScale, RoundingMode.HALF_EVEN)
      fromOverBase.multiply(baseOverTo)
    } else {
      baseOverTo
    }
  }

}


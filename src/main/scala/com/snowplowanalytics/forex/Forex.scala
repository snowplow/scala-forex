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
// Joda time
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

case class Forex(config: ForexConfig) {
  val client = ForexClient.getOerClient(config)
  // target currency
  var to   = config.baseCurrency
  // default value for currency conversion is 1 unit of the source currency
  var conversionAmount  = new BigDecimal(1) 
  // flag which determines whether to get the exchange rate on previous day or on the closer day 
  val getNearestDay     = config.getNearestDay
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
    from = config.baseCurrency
    ForexLookupTo(this)
  }

  /**
  * starts building a fluent interface, 
  * performs currency look up from the desired currency
  * @param currency - CurrencyUnit type
  * @returns ForexLookupTo object which is the part of the fluent interface
  */
  def rate(currency: CurrencyUnit): ForexLookupTo = {
    from = currency
    ForexLookupTo(this)
  }

  // wrapper method for rate(CurrencyUnit)
  def rate(currency: String): ForexLookupTo = {
    rate(CurrencyUnit.getInstance(currency))
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
    conversionAmount = new BigDecimal(amount)
    rate
  }
  
  def convert(amount: Int, currency: CurrencyUnit): ForexLookupTo = {
    conversionAmount = new BigDecimal(amount)
    rate(currency)
  }
  // wrapper method for convert(Int, CurrencyUnit)
  def convert(amount: Int, currency: String): ForexLookupTo = {      
    convert(amount, CurrencyUnit.getInstance(currency))
  }
}


/**
 * ForexLookupTo is part of the fluent interface,
 * it only has method to which returns ForexLookupWhen object 
 * which will then be passed to ForexLookupWhen class
 * @pvalue fx - Forex object which was configured ealier 
 */
case class ForexLookupTo(fx: Forex) {
  
  /**
   * this method sets the target currency to the desired one
   * @param currency - target currency
   * @return ForexLookupWhen object which is part of the fluent interface
   */
  def to(currency: CurrencyUnit): ForexLookupWhen = {
    fx.to = currency
    ForexLookupWhen(fx)
  }
  // wrapper method for to(CurrencyUnit)
  def to(currency: String): ForexLookupWhen = {
    to(CurrencyUnit.getInstance(currency))
  }

}

/**
* ForexLookupWhen is the end of the fluent interface,
* methods in this class are the final stage of currency lookup and conversion
* and return error mesage as string if an exception is caught 
* or Money if an exchange rate is found 
* note that the source currency is set to the base currency after each lookup or conversion
* same for the conversion amount, which is set to 1 unit 
* @pvalue fx - Forex object 
*/
case class ForexLookupWhen(fx: Forex) {
  val conversionAmt = if (fx.conversionAmount != new BigDecimal(1)) 
                          fx.conversionAmount                        
                      else 
                          new BigDecimal(1)
                        
  val fromCurr = fx.from 
  val toCurr   = fx.to
  // money in a given amount 
  val moneyInSourceCurrency = BigMoney.of(fromCurr, conversionAmt)
  /**
  * perform live currency look up or conversion, no caching needed
  */
  def now: Either[String, Money] =  {
    try {
      fx.from = fx.config.baseCurrency
      fx.conversionAmount = new BigDecimal(1)
      val baseOverFrom = fx.client.getCurrencyValue(fromCurr) 
      val baseOverTo = fx.client.getCurrencyValue(toCurr) 
      val rate = getForexRate(baseOverFrom, baseOverTo)
      Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
    } catch {
      case (e: FileNotFoundException) => Left("exchange rate unavailable")
      case (e: MalformedURLException) => Left("invalid URL")
      case (e: IOException)           => Left(e.getMessage)
    }
  }
  
  
  /**
  * a cached version of the live exchange rate is used, 
  * if the timestamp of that exchange rate is less than 
  * or equal to `nowishSecs` old. Otherwise a new lookup is performed.
  */
  def nowish: Either[String, Money] = {
    try {
      fx.from = fx.config.baseCurrency
      fx.conversionAmount = new BigDecimal(1)
      val nowishTime = DateTime.now.minusSeconds(fx.config.nowishSecs)
      fx.client.nowishCache.get((fromCurr, toCurr)) match {
        // from:to found in LRU cache
        case Some(tpl) => {
          println("found in nowish cache")
          val (timeStamp, exchangeRate) = tpl
          if (nowishTime.isBefore(timeStamp) || nowishTime.equals(timeStamp)) {
             // the timestamp in the cache is within the allowed range 
            Right(moneyInSourceCurrency.convertedTo(toCurr, exchangeRate).toMoney(RoundingMode.HALF_EVEN))
          } else {
            //update the exchange rate  
            getLiveRateAndUpdateCache
          }
        }
        // from:to not found in LRU
        case None => {
          fx.client.nowishCache.get((toCurr, fromCurr)) match {
            // to:from found in LRU
            case Some(tpl) => { 
               println("inverse found in nowish cache")
              val (time, rate) = tpl
              val inverseRate = new BigDecimal(1).divide(rate, fx.commonScale, RoundingMode.HALF_EVEN)
              Right(moneyInSourceCurrency.convertedTo(toCurr, inverseRate).toMoney(RoundingMode.HALF_EVEN))
            }
            // Neither direction found in LRU
            case None => {
              getLiveRateAndUpdateCache
            }
          }
        }
      }
   } catch {
      case (e: FileNotFoundException) => Left("exchange rate unavailable")
      case (e: MalformedURLException) => Left("invalid URL")
      case (e: IOException) =>           Left(e.getMessage)
    }
  }

  /*
  * gets live exchange rate and put it in the nowish cache
  */
  private def getLiveRateAndUpdateCache: Either[String, Money]= {
    val live = now
    live match {
      case Left(errorMessage) => live
      case Right(forexMoney)  => fx.client.nowishCache.put((fromCurr, toCurr), (DateTime.now, forexMoney.getAmount))  
    }
    live
  }

  /**
  * gets the latest end-of-day rate prior to the datetime by default or
  * on the closer day if the getNearestDay flag is true, caching is available
  */
  def at(tradeDate: DateTime): Either[String, Money] = {
    fx.from = fx.config.baseCurrency
    fx.conversionAmount = new BigDecimal(1)
    val latestEod = if (fx.getNearestDay == EodRoundUp) {
      tradeDate.withTimeAtStartOfDay.plusDays(1)
    } else {
      tradeDate.withTimeAtStartOfDay
    }
    eod(latestEod)   
  }

  /**
  * gets the end-of-day rate for the specified day, caching is available
  */
  def eod(eodDate: DateTime): Either[String, Money] = { 
    try {
      fx.from = fx.config.baseCurrency
      fx.conversionAmount = new BigDecimal(1)
      fx.client.eodCache.get((fromCurr, toCurr, eodDate)) match {
      
        case Some(rate) => 
                            Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
        case None       =>  
                            var rate = new BigDecimal(1)
                            fx.client.eodCache.get((toCurr, fromCurr, eodDate)) match {                  
                              case Some(exchangeRate) =>                                              
                                                 rate = new BigDecimal(1).divide(exchangeRate, fx.commonScale, RoundingMode.HALF_EVEN)
                              case None =>
                                                 rate = getHistoricalRate(eodDate)
                                                 fx.client.eodCache.put((fromCurr, toCurr, eodDate), rate)            
                            }
                            Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
      }
    } catch {
        case (e: FileNotFoundException) => Left("exchange rate unavailable")
        case (e: MalformedURLException) => Left("invalid URL")
        case (e: IOException) =>           Left(e.getMessage)
    }
  }

  /**
  * gets historical forex rate between two currencies on a given date  
  * @returns exchange rate as BigDecimal 
  */
  private def getHistoricalRate(date: DateTime): BigDecimal = {
    val baseOverFrom = fx.client.getHistoricalCurrencyValue(fromCurr, date)
    val baseOverTo   = fx.client.getHistoricalCurrencyValue(toCurr, date)
    getForexRate(baseOverFrom, baseOverTo)
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


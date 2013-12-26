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

// Joda time
import org.joda.time._
import org.joda.money._

// Scala
import scala.collection.JavaConversions._

// LRUCache
import com.twitter.util.LruMap

import java.net.MalformedURLException
import java.io.FileNotFoundException
import java.io.IOException
 
/**
 * Starts building the fluent interface for currency look-up and conversion 
 */

case class Forex(config: ForexConfig) {


  val client = ForexClient.getClient(config.appId)
  // LRU cache for nowish request, with tuple of source currency and target currency as the key
  // and tuple of time and exchange rate as the value 
  val nowishCache = if (config.nowishCacheSize > 0) 
                          new LruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize)
                    else null
  // LRU cache for historical request, with triple of source currency, target currency and time as the key 
  // and exchange rate as the value
  val historicalCache = if (config.historicalCacheSize > 0)
                            new LruMap[HistoricalCacheKey, HistoricalCacheValue](config.historicalCacheSize)
                        else null
  // currency to be converted
  var from = config.baseCurrency
  // target currency
  var to:Option[CurrencyUnit]   = None

  // default value for currency conversion is 1 unit of the source currency
  var conversionAmount  = new BigDecimal(1) 

  // flag which determines whether to get the exchange rate on previous day or on the closer day 
  val getNearestDay     = config.getNearestDay

  // preserve 10 digits after decimal point of a number when performing division 
  val maxScale         = 10 
  // usually the number of digits of a currency value has only 6 digits 
  val commonScale      = 6 

  /**
  * starts building a currency look up from the desired currency, 
  * or from USD if the source currency is not given. 
  * @param currency - the source currency 
  * @return ForexLookupTo object which is the start of the fluent interface
  */
  def rate: ForexLookupTo = {
    if (from == None) {
      throw new IllegalArgumentException("baseCurrency and source currency cannot both be null")
    } 
    ForexLookupTo(this)
  }

  def rate(currency: CurrencyUnit): ForexLookupTo = {
    from = Some(currency)
    ForexLookupTo(this)
  }

  // wrapper method for rate
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
   * @param currency - The *source* currency.
   * (The target currency will be supplied
   * to the ForexLookupTo later).
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
  // wrapper method 
  def convert(amount: Int, currency: String): ForexLookupTo = {
    convert(amount, CurrencyUnit.getInstance(currency))
  }
}


/**
 * ForexLookupTo is part of the fluent interface
 * @pvalue fx - Forex object which is returned from the methods in Forex class
 */
case class ForexLookupTo(fx: Forex) {
  
  /**
   * this method sets the target currency to the desired one
   * @param currency - target currency
   * @return ForexLookupWhen object which is part of the fluent interface
   */
  def to(currency: CurrencyUnit): ForexLookupWhen = {
    fx.to = Some(currency)
    ForexLookupWhen(fx)
  }
  // wrapper method
  def to(currency: String): ForexLookupWhen = {
    to(CurrencyUnit.getInstance(currency))
  }

}

/**
* ForexLookupWhen is the end of the fluent interface,
* methods in this class perform currency lookup and conversion
* @pvalue fx - Forex object returned from the methods in the ForexLookupTo class
* methods return error mesage as string if an exception is caught 
* or Money if an exchange rate is available 
*/
case class ForexLookupWhen(fx: Forex) {
  // if the amount is specified this time, we need to set the amount to 1 for next time
  val conversionAmt = if (fx.conversionAmount != new BigDecimal(1)) 
                          fx.conversionAmount                        
                      else 
                          new BigDecimal(1)
                        
  val Some(fromCurr) = fx.from 
  val Some(toCurr)   = fx.to
  // money in a given amount 
  val moneyInSourceCurrency = BigMoney.of(fromCurr, conversionAmt)

  /**
  * perform live currency look up or conversion, no chaching needed
  */
  def now: Either[String, Money] =  {
      try {
        fx.from = fx.config.baseCurrency
        fx.conversionAmount = new BigDecimal(1)
        val usdOverFrom = fx.client.getCurrencyValue(fromCurr)            
        val usdOverTo   = fx.client.getCurrencyValue(toCurr)
        val rate        = getForexRate(usdOverFrom, usdOverTo)
        Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
      } catch {
        case (e: FileNotFoundException) => Left("exchange rate unavailable")
        case (e: MalformedURLException) => Left("invalid URL")
        case (e: IOException) =>           Left(e.getMessage)
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
      fx.nowishCache.get((fromCurr, toCurr)) match {
        // from:to found in LRU cache
        case Some(tpl) => {
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
          fx.nowishCache.get((toCurr, fromCurr)) match {
            // to:from found in LRU
            case Some(tpl) => { 
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
  * gets live exchange rate and put it in the cache
  */
  private def getLiveRateAndUpdateCache: Either[String, Money]= {
    val live = now
    live match {
      case Left(errorMessage) => live
      case Right(forexMoney)  => fx.nowishCache.put((fromCurr, toCurr), (DateTime.now, forexMoney.getAmount))  
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
      fx.historicalCache.get((fromCurr, toCurr, eodDate)) match {
      
        case Some(rate) => 
                            Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
        case None       =>  
                            var rate = new BigDecimal(1)
                            fx.historicalCache.get((toCurr, fromCurr, eodDate)) match {                  
                              case Some(exchangeRate) =>                                              
                                                 rate = new BigDecimal(1).divide(exchangeRate, fx.commonScale, RoundingMode.HALF_EVEN)
                              case None =>
                                                 rate = getHistoricalRate(eodDate)
                                                 fx.historicalCache.put((fromCurr, toCurr, eodDate), rate)            
                            }
                            Right(moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN))
      }
    } catch {
        case (e: FileNotFoundException) => Left("exchange rate unavailable")
        case (e: MalformedURLException) => Left("invalid URL")
        case (e: IOException) =>           Left(e.getMessage)
    }
  }

  /**get historical forex rate between two currencies on a given date  
  * @returns exchange rate as BigDecimal 
  */
  private def getHistoricalRate(date: DateTime): BigDecimal = {
    val dateCal     = date.toGregorianCalendar
    val usdOverTo   = fx.client.getHistoricalCurrencyValue(toCurr, dateCal)
    val usdOverFrom = fx.client.getHistoricalCurrencyValue(fromCurr, dateCal)
    getForexRate(usdOverFrom, usdOverTo)
  }

  /** get the forex rate between source currency and target currency
  * @returns ratio of from:to as BigDecimal
  */
  private def getForexRate(usdOverFrom: BigDecimal, usdOverTo: BigDecimal): BigDecimal = {
    if (fromCurr != CurrencyUnit.USD) {
      val fromOverUsd = new BigDecimal(1).divide(usdOverFrom, fx.commonScale, RoundingMode.HALF_EVEN)
      fromOverUsd.multiply(usdOverTo)
    } else {
      usdOverTo
    }
  }

}

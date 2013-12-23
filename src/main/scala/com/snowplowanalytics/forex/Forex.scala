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

// Scala
import scala.collection.JavaConversions._

// LRUCache
import com.twitter.util.LruMap

// This project
import oerclient._

 
// TODO: should we ask what version of the API the user has access to?
// Because e.g. Enterprise is more powerful than Developer. Because
// Enterprise allows a baseCurrency to be set. Which means that
// conversions are easier.
// If a baseCurrency can't be set, then for EUR -> GBP, I have to convert
// EUR -> USD -> GBP. Not very nice!



/**
 * Forex is 
 *
 */

case class Forex(config: ForexConfig) {


  val client = new OerJsonClient(config.appId)

  val nowishCache = if (config.nowishCacheSize > 0) 
                          new LruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize)
                    else null
  
  val historicalCache = if (config.historicalCacheSize > 0)
                            new LruMap[HistoricalCacheKey, HistoricalCacheValue](config.historicalCacheSize)
                        else null

  var from = config.baseCurrency
  
  var to:Option[String]   = None

  // default value for currency conversion is 1 unit of the source currency
  var conversionAmount  = 1 

  val getNearestDay     = config.getNearestDay

  // preserve 10 digits after decimal point of a number when performing division 
  val max_scale         = 10 // TODO: change C-style max_scale etc to maxScale etc
  // usually the number of digits of a currency value has only 6 digits 
  val common_scale      = 6 // TODO: change C-style max_scale etc to maxScale etc

  def setSourceCurrency(source: String): Forex = {
    from = Some(source)
    this
  }

  def setTargetCurrency(target: String): Forex = {
    to = Some(target)
    this
  }

  def setConversionAmount(amount: Int): Forex = {
    conversionAmount = amount
    this
  }

  // rate method leaves the source currency to be default value(i.e. USD) 
  //and returns ForexLookupTo object
  def rate: ForexLookupTo = {
    if (from == None) {
      throw new IllegalArgumentException("baseCurrency and source currency cannot both be null")
    } 
    var Some(curr) = config.baseCurrency
    var forex = setSourceCurrency(curr)
    ForexLookupTo(forex)
  }

  // rate method sets the source currency to a specific currency
  // and returns ForexLookupTo object
  def rate(currency: String): ForexLookupTo = {
    var forex = setSourceCurrency(currency)
    ForexLookupTo(forex)
  }

  def convert(amount: Int): ForexLookupTo = {
    setConversionAmount(amount)
    rate
  }

  /**
   * Starts building a currency conversion from
   * the supplied currency, for the supplied
   * amount. Returns a ForexLookupTo to finish
   * the conversion.
   *
   * @param amount The amount of currency to
   * convert
   * @param currency The *source* currency.
   * (The target currency will be supplied
   * to the ForexLookupTo later).
   * @returns a ForexLookupTo, part of the
   * currency conversion fluent interface.
   */
  def convert(amount: Int, currency: String): ForexLookupTo = {
    setConversionAmount(amount)
    rate(currency)
  }
}


/**
 * Describe this here
 *
 * @pvalue fx - TODO desc what fx is
 */
case class ForexLookupTo(fx: Forex) {
  
  /**
   * TODO
   */
  def to(currency: String): ForexLookupWhen = {
    var forex = fx.setTargetCurrency(currency)
    ForexLookupWhen(forex)
  }

}

case class ForexLookupWhen(fx: Forex) {
  var conversionAmt = 1

  def now(): BigDecimal =  {
    val Some(fromCurr) = fx.from 
    val Some(toCurr)   = fx.to 
    // if the amount is specified this time, we need to set the amount to 1 for next time
    if (fx.conversionAmount > 1) { 
        conversionAmt = fx.conversionAmount
       fx.setConversionAmount(1)
    } 
    
      if (fx.from != Some("USD")) {
    
        val fromOverUSD = new BigDecimal(1).divide(fx.client.getCurrencyValue(fromCurr)
                , fx.max_scale, RoundingMode.HALF_EVEN)

        val usdOverTo = fx.client.getCurrencyValue(toCurr)

        fromOverUSD.multiply(usdOverTo).multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
    
      } else {
    
        fx.client.getCurrencyValue(toCurr).multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
    
      }


  }
  

  def nowish: BigDecimal = {

    val nowishTime = DateTime.now.minusSeconds(fx.config.nowishSecs)
    val Some(fromCurr) = fx.from 
    val Some(toCurr)   = fx.to 
    fx.nowishCache.get((fromCurr, toCurr)) match {
      // from:to found in LRU cache
      case Some(tpl) => {
        val (timeStamp, exchangeRate) = tpl

        if (nowishTime.isBefore(timeStamp)|| nowishTime.equals(timeStamp)) {
           exchangeRate
        } else {
          now()
        }
      }
      // from:to not found in LRU
      case None => {
        fx.nowishCache.get((toCurr, fromCurr)) match {
          // to:from found in LRU
          case Some(tpl) => { 
            val (time, rate) = tpl
            new BigDecimal(1).divide(rate, fx.max_scale, RoundingMode.HALF_EVEN)
                .multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
          }
          // Neither direction found in LRU
          case None => {
            val live = now().multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
            fx.nowishCache.put((fromCurr, toCurr), (DateTime.now, live))
            live
          }
        }
      }
    }
  }

  def at(tradeDate: DateTime): BigDecimal = {
    val latestEod = if (fx.getNearestDay == EodRoundUp) {
      tradeDate.withTimeAtStartOfDay.plusDays(1)
    } else {
      tradeDate.withTimeAtStartOfDay
    }
    eod(latestEod)   
  }


  def eod(eodDate: DateTime): BigDecimal = {
    val Some(fromCurr) = fx.from 
    val Some(toCurr)   = fx.to 
    fx.historicalCache.get((fromCurr, toCurr, eodDate)) match {
    
      case Some(rate) => 
                        rate.multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)// todo : rm duplicate
      case None       => 
                           fx.historicalCache.get((toCurr, fromCurr, eodDate)) match {
                            case Some(exchangeRate) =>
                                              
                                               new BigDecimal(1).divide(exchangeRate, fx.max_scale, RoundingMode.HALF_EVEN)
                                                  .multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
                            case None =>
                                               val rate = getHistoricalRate(fx, eodDate).multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
                                               fx.historicalCache.put((fromCurr, toCurr, eodDate), rate)
                                               rate
                        }
    }
  }

  private def getHistoricalRate(fx: Forex, date: DateTime): BigDecimal = {

    // If the amount is specified this time, we need to set the amount to 1 for next time
    if (fx.conversionAmount > 1) { 
      conversionAmt = fx.conversionAmount
      fx.setConversionAmount(1)
    } 
    val Some(fromCurr) = fx.from 
    val Some(toCurr)   = fx.to 
    val dateCal = date.toGregorianCalendar
    val usdOverTo = fx.client.getHistoricalCurrencyValue(toCurr, dateCal)
    if (fx.from != Some("USD")) {
      val rate = fx.client.getHistoricalCurrencyValue(fromCurr, dateCal)
      val fromOverUsd = new BigDecimal(1).divide(rate, fx.max_scale, RoundingMode.HALF_EVEN)
      fromOverUsd.multiply(usdOverTo).multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
    } else {
      usdOverTo.multiply(new BigDecimal(conversionAmt)).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
    }
  }

}

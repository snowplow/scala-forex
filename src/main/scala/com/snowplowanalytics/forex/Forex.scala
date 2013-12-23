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


  val client = ForexClient.getClient(config.appId)

  val nowishCache = if (config.nowishCacheSize > 0) 
                          new LruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize)
                    else null
  
  val historicalCache = if (config.historicalCacheSize > 0)
                            new LruMap[HistoricalCacheKey, HistoricalCacheValue](config.historicalCacheSize)
                        else null

  var from = config.baseCurrency
  
  var to:Option[CurrencyUnit]   = None

  // default value for currency conversion is 1 unit of the source currency
  var conversionAmount  = new BigDecimal(1) 

  val getNearestDay     = config.getNearestDay

  // preserve 10 digits after decimal point of a number when performing division 
  val max_scale         = 10 // TODO: change C-style max_scale etc to maxScale etc
  // usually the number of digits of a currency value has only 6 digits 
  val common_scale      = 6 // TODO: change C-style max_scale etc to maxScale etc

  def setSourceCurrency(source: CurrencyUnit): Forex = {
    from = Some(source)
    this
  }

  def setTargetCurrency(target: CurrencyUnit): Forex = {
    to = Some(target)
    this
  }

  def setConversionAmount(amount: Int): Forex = {
    conversionAmount = new BigDecimal(amount)
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
  def rate(currency: CurrencyUnit): ForexLookupTo = {
    var forex = setSourceCurrency(currency)
    setConversionAmount(1)
    ForexLookupTo(forex)
  }

  def rate(currency: String): ForexLookupTo = {
    setConversionAmount(1)
    rate(CurrencyUnit.getInstance(currency))
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

  def convert(amount: Int): ForexLookupTo = {
    setConversionAmount(amount)
    rate
  }
  
  def convert(amount: Int, currency: CurrencyUnit): ForexLookupTo = {
    setConversionAmount(amount)
    rate(currency)
  }

  def convert(amount: Int, currency: String): ForexLookupTo = {
    convert(amount, CurrencyUnit.getInstance(currency))
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
  def to(currency: CurrencyUnit): ForexLookupWhen = {
    var forex = fx.setTargetCurrency(currency)
    ForexLookupWhen(forex)
  }

  def to(currency: String): ForexLookupWhen = {
    to(CurrencyUnit.getInstance(currency))
  }

}

case class ForexLookupWhen(fx: Forex) {
  // if the amount is specified this time, we need to set the amount to 1 for next time
  val conversionAmt = if (fx.conversionAmount != new BigDecimal(1)) {
                          fx.conversionAmount
                        }
                      else 
                          new BigDecimal(1)
                        
  val Some(fromCurr) = fx.from 
  val Some(toCurr)   = fx.to
  val moneyInSourceCurrency = BigMoney.of(fromCurr, conversionAmt)

  def now: Money =  {
        val usdOverFrom = fx.client.getCurrencyValue(fromCurr)            
        val usdOverTo   = fx.client.getCurrencyValue(toCurr)
        val rate        = getForexRate(usdOverFrom, usdOverTo)
        moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN)
  }
  
  

  def nowish: Money = {
    val nowishTime = DateTime.now.minusSeconds(fx.config.nowishSecs)
    fx.nowishCache.get((fromCurr, toCurr)) match {
      // from:to found in LRU cache
      case Some(tpl) => {
        val (timeStamp, exchangeRate) = tpl
        if (nowishTime.isBefore(timeStamp) || nowishTime.equals(timeStamp)) {
           // the timestamp in the cache is within the allowed range 
          moneyInSourceCurrency.convertedTo(toCurr, exchangeRate).toMoney(RoundingMode.HALF_EVEN)
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
            val inverseRate = new BigDecimal(1).divide(rate, fx.common_scale, RoundingMode.HALF_EVEN)
            moneyInSourceCurrency.convertedTo(toCurr, inverseRate).toMoney(RoundingMode.HALF_EVEN)
          }
          // Neither direction found in LRU
          case None => {
            getLiveRateAndUpdateCache
          }
        }
      }
    }
  }
  private def getLiveRateAndUpdateCache: Money = {
    val live = now
    fx.nowishCache.put((fromCurr, toCurr), (DateTime.now, live.getAmount))
    live
  }


  def at(tradeDate: DateTime): Money = {
    val latestEod = if (fx.getNearestDay == EodRoundUp) {
      tradeDate.withTimeAtStartOfDay.plusDays(1)
    } else {
      tradeDate.withTimeAtStartOfDay
    }
    eod(latestEod)   
  }


  def eod(eodDate: DateTime): Money = { 
    fx.historicalCache.get((fromCurr, toCurr, eodDate)) match {
    
      case Some(rate) => 
                          moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN)
      case None       =>  
                          var rate = new BigDecimal(1)
                          fx.historicalCache.get((toCurr, fromCurr, eodDate)) match {                  
                            case Some(exchangeRate) =>                                              
                                               rate = new BigDecimal(1).divide(exchangeRate, fx.common_scale, RoundingMode.HALF_EVEN)
                            case None =>
                                               rate = getHistoricalRate(eodDate)
                                               fx.historicalCache.put((fromCurr, toCurr, eodDate), rate)            
                          }
                          moneyInSourceCurrency.convertedTo(toCurr, rate).toMoney(RoundingMode.HALF_EVEN)
    }
  }

  // get historical forex rate between two currencies on a given date  
  private def getHistoricalRate(date: DateTime): BigDecimal = {
     val dateCal     = date.toGregorianCalendar
     val usdOverTo   = fx.client.getHistoricalCurrencyValue(toCurr, dateCal)
     val usdOverFrom = fx.client.getHistoricalCurrencyValue(fromCurr, dateCal)
     getForexRate(usdOverFrom, usdOverTo)
  }

  // get the forex rate between source currency and target currency, output = from:to
  private def getForexRate(usdOverFrom: BigDecimal, usdOverTo: BigDecimal): BigDecimal = {
    if (fromCurr != CurrencyUnit.USD) {
      val fromOverUsd = new BigDecimal(1).divide(usdOverFrom, fx.common_scale, RoundingMode.HALF_EVEN)
      fromOverUsd.multiply(usdOverTo)
    } else {
      usdOverTo
    }
  }

}

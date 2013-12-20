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

// Java OER
import org.openexchangerates.oerjava.OpenExchangeRates
import org.openexchangerates.oerjava.Currency

// Joda time
import org.joda.time._

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
/*

Questions:

1. In rate method, when the user did not initialize neither 
home currency nor source currency, do I throw an exception and return? 
and where do I catch the exception?

//2. sizes of the two caches? 

//3. do I need to store the lookup results into cache after every http request?

*/



/**
 * Forex is a ???
 *
 */


case class ForexBuilder(appId: String) {

  // default values for optional fields
  private var _nowishCacheSize = 13530
  private var _nowishSecs      = 300
  private var _historicalCacheSize    = 405900 
  private var _getNearestDay   = false
  // there is no default value for home currency
  private var _baseCurrency = "null"
  
 def baseCurrency          = _baseCurrency
 def nowishCacheSize       = _nowishCacheSize
 def nowishSecs            = _nowishSecs
 def historicalCacheSize   = _historicalCacheSize
 def getNearestDay         = _getNearestDay

  def buildBaseCurrency(currency: String):  ForexBuilder = {
    _baseCurrency = currency
    this
  }

  def buildNowishCache(size: Int): ForexBuilder = {
    _nowishCacheSize = size
    this
  }

  def buildHistoricalCache(size: Int): ForexBuilder = {
    _historicalCacheSize    = size
    this
  }

  def buildNowishSecs(secs: Int): ForexBuilder = {
    _nowishSecs = secs
    this
  }

  def buildNearestDay: ForexBuilder = {
    _getNearestDay = true
    this
  }  

  def build: Forex = {
    new Forex(this)
  }


}



case class Forex(builder: ForexBuilder) {


  val client = OpenExchangeRates.getClient(builder.appId)

  val nowishCache = if (builder.nowishCacheSize > 0) 
                          new LruMap[NowishCacheKey, NowishCacheValue](builder.nowishCacheSize)
                    else null
  
  val historicalCache = if (builder.historicalCacheSize > 0)
                            new LruMap[HistoricalCacheKey, HistoricalCacheValue](builder.historicalCacheSize)
                        else null

  var from = if (builder.baseCurrency != "null") { builder.baseCurrency} else { "null" }
  
  var to   = "null"

  val getNearestDay = builder.getNearestDay

  // preserve 10 digits after decimal point of a number when performing division 
  val max_scale = 10
  // usually the number of digits of a currency value has only 6 digits 
  val common_scale     = 6

  def setSourceCurrency(source: String): Forex = {
    from = source
    this
  }

  def setTargetCurrency(target: String): Forex = {
    to = target
    this
  }



  // rate method leaves the source currency to be default value(i.e. USD) 
  //and returns ForexLookupTo object
  def rate: ForexLookupTo = {
    if (from == "null") {
      throw new IllegalArgumentException("baseCurrency and source currency cannot both be null")
    } 
    var forex = setSourceCurrency(builder.baseCurrency)
    ForexLookupTo(forex)
  }

  // rate method sets the source currency to a specific currency
  // and returns ForexLookupTo object
  def rate(currency: String): ForexLookupTo = {
    var forex = setSourceCurrency(currency)
    ForexLookupTo(this)
  }

}



case class ForexLookupTo(fx: Forex) {
  
  def to(currency: String): ForexLookupWhen = {
    var forex = fx.setTargetCurrency(currency)
    ForexLookupWhen(forex)
  }

}

case class ForexLookupWhen(fx: Forex) {

  def now: BigDecimal =  {

    if (fx.from != "USD") {
  
      val USDOverFrom = new BigDecimal(1).divide(fx.client.getCurrencyValue(fx.from)
              , fx.max_scale, RoundingMode.HALF_EVEN)

      val ToOverUSD = fx.client.getCurrencyValue(fx.to)

      USDOverFrom.multiply(ToOverUSD).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
  
    } else {
  
      fx.client.getCurrencyValue(fx.to)
  
    }
  }
  

  def nowish: BigDecimal = {

      val nowishTime    = DateTime.now.minusSeconds(fx.builder.nowishSecs)

      fx.nowishCache.get((fx.from, fx.to)) match {
        case Some(tuple) => 
                            val (timeStamp, exchangeRate) = tuple
                            if  (nowishTime.isBefore(timeStamp)
                                       || nowishTime.equals(timeStamp)) {
                               exchangeRate
                            } else {
                              now
                            }
        case None =>  
                      fx.nowishCache.get((fx.to, fx.from)) match {

                        case Some(value) =>   
                                    val (time, rate) = value
                                    new BigDecimal(1).divide(rate, fx.common_scale, RoundingMode.HALF_EVEN)
                                    
                        case None =>
                          val liveExchangeRate = now
                          fx.nowishCache.put((fx.from, fx.to), (DateTime.now, liveExchangeRate))
                          liveExchangeRate
                      }
      }
        
  }


  def at(tradeDate: DateTime): BigDecimal = {
    var latestEod = tradeDate.withTimeAtStartOfDay
    if (fx.getNearestDay) {
      latestEod   = latestEod.plusDays(1)
    }
    eod(latestEod)   
  }


  def eod(eodDate: DateTime): BigDecimal = {

    fx.historicalCache.get((fx.from, fx.to, eodDate)) match {
    
      case Some(rate) => 
                        rate
      case None       => 
                           fx.historicalCache.get((fx.to, fx.from, eodDate)) match {
                            case Some(exchangeRate) =>
                                              
                                               new BigDecimal(1).divide(exchangeRate, fx.common_scale, RoundingMode.HALF_EVEN)
                            case None =>
                                               val rate = getHistoricalRate(fx, eodDate)
                                               fx.historicalCache.put((fx.from, fx.to, eodDate), rate)
                                               rate
                        }
    }
  }

  private def getHistoricalRate(fx: Forex, date: DateTime): BigDecimal = {

      val dateCal = date.toGregorianCalendar

      if (fx.from != "USD") {
  
      val USDOverFrom = new BigDecimal(1).divide(fx.client.getHistoricalCurrencyValue(fx.from, dateCal)
              , fx.max_scale, RoundingMode.HALF_EVEN)

      val ToOverUSD = fx.client.getHistoricalCurrencyValue(fx.to, dateCal)

      USDOverFrom.multiply(ToOverUSD).setScale(fx.common_scale, RoundingMode.HALF_EVEN)
  
    } else {
  
      fx.client.getHistoricalCurrencyValue(fx.to, dateCal)
  
    }
  }


}

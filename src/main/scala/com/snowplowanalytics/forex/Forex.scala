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


// TODO: should homeCurrency be a String?

// TODO: should we ask what version of the API the user has access to?
// Because e.g. Enterprise is more powerful than Developer. Because
// Enterprise allows a homeCurrency to be set. Which means that
// conversions are easier.
// If a homecurrency can't be set, then for EUR -> GBP, I have to convert
// EUR -> USD -> GBP. Not very nice!
/*

few things to note: 

1. In rate method, when the user did not initialize neither 
home currency nor source currency, do I throw an exception and return? 
and where do I catch the exception?

2. sizes of the two caches?
*/

/**
 * Forex is a ???
 *
 */


case class ForexBuilder(appId: String) {

  // default values for optional fields
  private var _lruCacheSize = 60000
  private var _nowishSecs   = 300
  // there is no default value for home currency
  private var _homeCurrency = Currency.NULL
  
   def homeCurrency   = _homeCurrency
   def lruCacheSize   = _lruCacheSize
   def nowishSecs     = _nowishSecs

  def buildHomeCurrency(currency: Currency):  ForexBuilder = {
    _homeCurrency = currency
    this
  }

  def buildLruCache(size: Int): ForexBuilder = {
    _lruCacheSize = size
    this
  }

  def buildNowishSecs(secs: Int): ForexBuilder = {
    _nowishSecs = secs
    this
  }

  def build: Forex = {
    new Forex(this)
  }


}



case class Forex(builder: ForexBuilder) {


  val client = OpenExchangeRates.getClient(builder.appId)

  //lruCache stores Tuple[from, to] as key and Tuple[timestamp, exchange rate] as value 
  val lruCache = new LruMap[CacheKey, CacheValue](builder.lruCacheSize)
  
  var from = if (builder.homeCurrency != null) { builder.homeCurrency} else { Currency.NULL }
  
  var to   = Currency.NULL

  // max possible number of digits of a currency value 
  val max_possible_precision = 15

  def setSourceCurrency(source: Currency): Forex = {
    from = source
    this
  }

  def setTargetCurrency(target: Currency): Forex = {
    to = target
    this
  }
  
  // rate method leaves the source currency to be default value(i.e. USD) 
  //and returns ForexLookupTo object
  def rate: ForexLookupTo = {
    if (from == Currency.NULL) {
      throw new IllegalArgumentException
    } 
    var forex = setSourceCurrency(from)
    ForexLookupTo(forex)
  }

  // rate method sets the source currency to a specific currency
  // and returns ForexLookupTo object
  def rate(currency: Currency): ForexLookupTo = {
    var forex = setSourceCurrency(currency)
    ForexLookupTo(forex)
  }

}



case class ForexLookupTo(fx: Forex) {
  
  def to(currency: Currency): ForexLookupWhen = {
    var forex = fx.setTargetCurrency(currency)
    ForexLookupWhen(forex)
  }

}

case class ForexLookupWhen(fx: Forex) {

  def now: BigDecimal =  {

    if (fx.from != Currency.USD) {
  
      val USDOverFrom = new BigDecimal(1).divide(fx.client.getCurrencyValue(fx.from)
              , fx.max_possible_precision, RoundingMode.HALF_EVEN)

      val ToOverUSD = fx.client.getCurrencyValue(fx.to)

      USDOverFrom.multiply(ToOverUSD).setScale(6, RoundingMode.HALF_EVEN)
  
    } else {
  
      fx.client.getCurrencyValue(fx.to)
  
    }
  }
  

  def nowish: BigDecimal = {

      val lruCache = fx.lruCache
      val nowishTime = DateTime.now.minusSeconds(fx.builder.nowishSecs)
      lruCache.get((fx.from, fx.to)) match {
        case Some(tuple) => 
                            val (timeStamp, exchangeRate) = tuple
                            if (nowishTime.isBefore(timeStamp)
                               || nowishTime.equals(timeStamp)) {
                              exchangeRate
                            } else {
                              now
                            }
        case None =>
                      val liveExchangeRate = now
                      lruCache.put((fx.from, fx.to), (DateTime.now, liveExchangeRate))
                      liveExchangeRate
      }
        
  }


  // def at(tradeDate: DateTime): BigDecimal = {

  // }

  // def eod(eodDate: DateTime): BigDecimal = {

  // }
}
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



// TODO: should homeCurrency be a String?

// TODO: should we ask what version of the API the user has access to?
// Because e.g. Enterprise is more powerful than Developer. Because
// Enterprise allows a homeCurrency to be set. Which means that
// conversions are easier.
// If a homecurrency can't be set, then for EUR -> GBP, I have to convert
// EUR -> USD -> GBP. Not very nice!
/*
few things to note: 
1. I added target currency field 
*/

/**
 * Forex is a ???
 *
 */


case class ForexBuilder(appId: String) {

  // default values for optional fields
  private var _homeCurrency = Currency.USD
  private var _lruCache     = 60000
  private var _nowishSecs   = 300
  private var _targetCurrency = Currency.USD

  def buildHomeCurrency(currency: Currency):  ForexBuilder = {
    _homeCurrency = currency
    this
  }

  def buildLruCache(size: Int): ForexBuilder = {
    _lruCache = size
    this
  }

  def buildNowishSecs(secs: Int): ForexBuilder = {
    _nowishSecs = secs
    this
  }

  def buildTargetCurrency(currency: Currency): ForexBuilder = {
    _targetCurrency = currency
    this
  }

  def build: Forex = {
    new Forex(this)
  }

  def homeCurrency   = _homeCurrency
  def lruCache       = _lruCache
  def nowishSecs     = _nowishSecs
  def targetCurrency = _targetCurrency
}



case class Forex(builder: ForexBuilder) {


  lazy val client = OpenExchangeRates.getClient(builder.appId)

  // max possible number of digits of a currency value 
  val max_possible_precision = 15


  // rate method returns ForexLookupFrom object with home currency sets to default value(USD)
  def rate: ForexLookupTo = {
    ForexLookupTo(builder)
  }

  // rate method sets the home currency to a specific currency
  def rate(currency: Currency): ForexLookupTo = {
    builder.buildHomeCurrency(currency)
    ForexLookupTo(builder)
  }

}



case class ForexLookupTo(fxbuilder: ForexBuilder) {

  def to(currency: Currency): ForexLookupWhen = {
    fxbuilder.buildTargetCurrency(currency)
    ForexLookupWhen(fxbuilder)
  }

}

case class ForexLookupWhen(fxbuilder: ForexBuilder) {

  def now: BigDecimal =  {
    val fx = fxbuilder.build
  
    if (fxbuilder.homeCurrency != Currency.USD) {
  
      val homeCurrOverUSD = new BigDecimal(1).divide(fx.client.getCurrencyValue(fxbuilder.homeCurrency)
              , fx.max_possible_precision, RoundingMode.HALF_UP)

      val USDoverTargetCurr = fx.client.getCurrencyValue(fxbuilder.targetCurrency)

      homeCurrOverUSD.multiply(USDoverTargetCurr).setScale(6, RoundingMode.HALF_EVEN)
  
    } else {
  
      fx.client.getCurrencyValue(fxbuilder.targetCurrency)
  
    }
  }
   
  // def nowish: BigDecimal = {

  // }

  // def nowish(nowishSecs: Int): BigDecimal = {

  // }

  // def at(tradeDate: DateTime): BigDecimal = {

  // }

  // def eod(eodDate: DateTime): BigDecimal = {

  // }
}
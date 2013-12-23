/*cation

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
// Scala
import scala.collection.JavaConversions._

// Specs2
import org.specs2.mutable.Specification

// Joda time
import org.joda.time._
import org.joda.money._


class ScalaOerTest extends Specification { 

    val oer = ForexClient.getClient(System.getProperty("forex.key"))

    val fx = new Forex(new ForexConfig(System.getProperty("forex.key"), baseCurrency = Some(CurrencyUnit.USD)))

    val cnyTogbpNow = fx.rate(CurrencyUnit.getInstance("CNY")).to(CurrencyUnit.GBP).now
     "CNY/GBP live rate [%s]".format(cnyTogbpNow) should {
       "be smaller than 1 and greater than 0" in { 
        cnyTogbpNow.isLessThan(Money.of(CurrencyUnit.GBP, 1))
      }
    }


  val gbpOvercnyNowish = fx.rate(CurrencyUnit.getInstance("CNY")).to(CurrencyUnit.GBP).nowish
  "CNY/GBP near-live rate [%s]".format(gbpOvercnyNowish) should {
    "be smaller than 1 and greater than 0, nowishCache size = [%s]".format(fx.nowishCache.size) in {
        gbpOvercnyNowish.isLessThan(Money.of(CurrencyUnit.GBP, 1))
    }
  }

  val tradeDate = new DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
  val gbpLatestEodRate = fx.rate.to(CurrencyUnit.GBP).at(tradeDate)

  "USD to GBP latest eod rate [%s]".format(gbpLatestEodRate) should {
    "be > 0, historicalCache size = [%s]".format(fx.historicalCache.size) in {
        gbpLatestEodRate.isPositive
    }
  }


  val eodDate = new DateTime(2011, 3, 13, 0, 0)
  val gbpEodRate =  fx.rate.to(CurrencyUnit.GBP).eod(eodDate)

  "USD to GBP eod rate [%s]".format(gbpEodRate) should {
    "be > 0, historicalCache size = [%s]".format(fx.historicalCache.size) in {
        gbpEodRate.isPositive
    }
  }


  val tradeInYenHistorical = fx.convert(10000).to(CurrencyUnit.JPY).eod(eodDate) 
  "convert 10000 USD dollars to Yen in 2011 = [%s]".format(tradeInYenHistorical) should {
    "be > 10000" in {
      tradeInYenHistorical.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN))
    }
  }

  val gbpOverCnyHistorical = fx.rate(CurrencyUnit.getInstance("CNY")).to(CurrencyUnit.GBP).at(tradeDate)

    "CNY to GBP latest eod rate [%s]".format(gbpOverCnyHistorical) should {
    "be > 0, historicalCache size = [%s]".format(fx.historicalCache.size) in {
        gbpOverCnyHistorical.isPositive
    }
  }


  val tradeInYenNow = fx.convert(10000).to(CurrencyUnit.JPY).now
  "convert 10000 USD dollars to Yen now = [%s]".format(tradeInYenNow) should {
    "be > 10000" in {
      tradeInYenNow.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN))
    }
  }

}

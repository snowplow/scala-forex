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

// Scala
import scala.collection.JavaConversions._

// Specs2
import org.specs2.mutable.Specification

// Joda time
import org.joda.time._

// This project
import oerclient._

class ScalaOerTest extends Specification { 

    val oer = OpenExchangeRates.getClient(System.getProperty("forex.key")) 

    val fx = new Forex(new ForexConfig(System.getProperty("forex.key"), baseCurrency = Some("USD")))

    val cnyTogbpNow = fx.rate("CNY").to("GBP").now
     "CNY/GBP live rate [%s]".format(cnyTogbpNow) should {
       "be smaller than 1 and greater than 0" in { 
        cnyTogbpNow must be < (new BigDecimal(1))
         cnyTogbpNow must be > (new BigDecimal(0))
      }
   }


  val gbpOvercnyNowish = fx.rate("CNY").to("GBP").nowish
  "CNY/GBP near-live rate [%s]".format(gbpOvercnyNowish) should {
    "be smaller than 1 and greater than 0, nowishCache size = [%s]".format(fx.nowishCache.size) in {

        gbpOvercnyNowish must be < (new BigDecimal(1))
        gbpOvercnyNowish must be > (new BigDecimal(0))
    }
  }

  val tradeDate = new DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
  val gbpLatestEodRate = fx.rate.to("GBP").at(tradeDate)

  "USD to GBP latest eod rate [%s]".format(gbpLatestEodRate) should {
    "be > 0, historicalCache size = [%s]".format(fx.historicalCache.size) in {
        gbpLatestEodRate must be > (new BigDecimal(0))
    }
  }


  val eodDate = new DateTime(2011, 3, 13, 0, 0)
  val gbpEodRate =  fx.rate.to("GBP").eod(eodDate)

  "USD to GBP eod rate [%s]".format(gbpEodRate) should {
    "be > 0, historicalCache size = [%s]".format(fx.historicalCache.size) in {
        gbpEodRate must be > (new BigDecimal(0))
    }
  }


  val tradeInYenHistorical = fx.convert(10000).to("JPY").eod(eodDate) 
  "convert 10000 USD dollars to Yen in 2011 = [%s]".format(tradeInYenHistorical) should {
    "be > 10000" in {
      tradeInYenHistorical must be > (new BigDecimal(10000))
    }
  }

  val gbpOverCnyHistorical = fx.rate("CNY").to("GBP").at(tradeDate)

    "CNY to GBP latest eod rate [%s]".format(gbpOverCnyHistorical) should {
    "be > 0, historicalCache size = [%s]".format(fx.historicalCache.size) in {
        gbpOverCnyHistorical must be > (new BigDecimal(0))
    }
  }


  val tradeInYenNow = fx.convert(10000).to("JPY").now
  "convert 10000 USD dollars to Yen now = [%s]".format(tradeInYenNow) should {
    "be > 10000" in {
      tradeInYenNow must be > (new BigDecimal(10000))
    }
  }

}

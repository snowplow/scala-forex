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

// Java OER
//import org.openexchangerates.oerjava.OpenExchangeRates
import org.openexchangerates.oerjava._

// Scala
import scala.collection.JavaConversions._

// Specs2
import org.specs2.mutable.Specification

// Joda time
import org.joda.time._

class ScalaOerTest extends Specification { 

   val oer = OpenExchangeRates.getClient(System.getProperty("forex.key")) 

    val fx = ForexBuilder(System.getProperty("forex.key")).buildBaseCurrency("USD").build


    val gbpNow = fx.rate("CNY").to("GBP").now
     "CNY/GBP live rate [%s]".format(gbpNow) should {
       "be smaller than 1 and greater than 0" in { 
        gbpNow must be < (new BigDecimal(1))
         gbpNow must be > (new BigDecimal(0))
      }
   }

  val gbpNowish = fx.rate.to("GBP").nowish
  "USD/GBP near-live rate [%s]".format(gbpNowish) should {
    "be smaller than 1 and greater than 0, nowishCache.size = [%s]".format(fx.nowishCache.size) in {

        gbpNowish must be < (new BigDecimal(1))
        gbpNowish must be > (new BigDecimal(0))
    }
  }

  val gbpOvercnyNowish = fx.rate("CNY").to("GBP").nowish
  "CNY/GBP near-live rate [%s]".format(gbpOvercnyNowish) should {
    "be smaller than 1 and greater than 0, nowishCache size = [%s]".format(fx.nowishCache.size) in {

        gbpNowish must be < (new BigDecimal(1))
        gbpNowish must be > (new BigDecimal(0))
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

  val gbpOverCnyHistorical = fx.rate("CNY").to("GBP").at(tradeDate)

    "CNY to GBP latest eod rate [%s]".format(gbpOverCnyHistorical) should {
    "be > 0, historicalCache size = [%s]".format(fx.historicalCache.size) in {
        gbpOverCnyHistorical must be > (new BigDecimal(0))
    }
  }
  
}
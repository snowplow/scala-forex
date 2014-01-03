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
// Scala
import scala.collection.JavaConversions._
// Specs2
import org.specs2.mutable.Specification
// Joda 
import org.joda.time._
import org.joda.money._

/**
* testing method for getting the latest end-of-day rate 
* prior to the datetime or the day after according to the user's setting 
*/
class ForexAtSpecification extends Specification { 
  val fx  = TestHelper.fx 
  val tradeDate = new DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
  val gbpLatestEodRate = fx.rate.to(CurrencyUnit.GBP).at(tradeDate)
  "this conversion" should {
    "always result in a Right" in {
      gbpLatestEodRate.isRight  
    }
  }
  val gbpmoney = gbpLatestEodRate.right.get

  "USD to GBP latest eod rate [%s]".format(gbpmoney) should {
    "be > 0" in {
        gbpmoney.isPositive
    }
  }
  

  val cnyOverGbpHistorical = fx.rate(CurrencyUnit.getInstance("CNY")).to(CurrencyUnit.GBP).at(tradeDate)
  "this conversion" should {
    "always result in a Right" in {
      cnyOverGbpHistorical.isRight  
    }
  }
  val cnyTogbpmoney = cnyOverGbpHistorical.right.get

  "CNY to GBP latest eod rate [%s]".format(cnyTogbpmoney) should {
    "be > 0" in {
      cnyTogbpmoney.isPositive
    }
  }
  
}
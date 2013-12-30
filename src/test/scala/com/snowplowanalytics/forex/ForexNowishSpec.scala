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

class ForexNowishSpec extends Specification { 
  val fx  = TestHelper.fx 
  val cnyOverGbpNowish = fx.rate(CurrencyUnit.getInstance("CNY")).to(CurrencyUnit.GBP).nowish
  
  "this conversion" should {
    "always result in a Right" in {
      cnyOverGbpNowish.isRight  
    }
  }
  val gbpmoney = cnyOverGbpNowish.right.get
   "CNY/GBP near-live rate [%s]".format(gbpmoney) should {
     "be smaller than 1, nowishCache size = [%s]".format(fx.client.nowishCache.size) in {
         gbpmoney.isLessThan(Money.of(CurrencyUnit.GBP, 1))
     }
  }
}


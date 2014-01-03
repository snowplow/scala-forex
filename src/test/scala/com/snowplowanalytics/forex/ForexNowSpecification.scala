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
// import scala.collection.JavaConversions._
// Specs2
import org.specs2.mutable.Specification
// Joda 
import org.joda.time._
import org.joda.money._

/**
* testing method for getting the live exchange rate
*/
class ForexNowSpecification extends Specification { 
  val fx  = TestHelper.fx 
  val tradeInYenNow = fx.convert(10000).to(CurrencyUnit.JPY).now
  "this conversion" should {
    "always result in a Right" in {
      tradeInYenNow.isRight  
    }
  }
  val jpymoney = tradeInYenNow.right.get
   "convert 10000 USD dollars to Yen now = [%s]".format(jpymoney) should {
     "be > 10000" in {
      jpymoney.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN))
     }
  }

  val cnyTogbpNow = fx.rate(CurrencyUnit.getInstance("CNY")).to(CurrencyUnit.GBP).now
  "this conversion" should {
    "always result in a Right" in {
      cnyTogbpNow.isRight  
    }
  }
  val gbpmoney = cnyTogbpNow.right.get 
  "CNY/GBP live rate [%s]".format(gbpmoney) should {
    "be smaller than 1 and greater than 0" in { 
      gbpmoney.isLessThan(Money.of(CurrencyUnit.GBP, 1))
    }
  }                        
  
}

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
import com.snowplowanalytics.forex.Forex
import com.snowplowanalytics.forex.ForexConfig
/**
* testing method for getting the approximate exchange rate
*/
class ForexNowishSpecification extends Specification { 
  val fx = new Forex(new ForexConfig(System.getenv("SBT_OPTS").split("=")(1), false))
  
  val cadOverGbpNowish = fx.rate(CurrencyUnit.getInstance("CAD")).to(CurrencyUnit.GBP).nowish
  
  "this conversion" should {
    "always result in a Right" in {
      cadOverGbpNowish.isRight  
    }
  }
  val gbpmoney = cadOverGbpNowish.right.get
   "CAD/GBP near-live rate [%s]".format(gbpmoney) should {
     "be smaller than 1" in {
         gbpmoney.isLessThan(Money.of(CurrencyUnit.GBP, 1))
     }
  }

  val jpyTogbpNowish = fx.rate(CurrencyUnit.getInstance("JPY")).to(CurrencyUnit.GBP).nowish
  "this conversion" should {
    "always result in a Right" in {
      jpyTogbpNowish.isRight  
    }
  }
  val jpyTogbpmoney = jpyTogbpNowish.right.get 
  "JPY/GBP live rate [%s]".format(jpyTogbpmoney) should {
    "be smaller than 1 and greater than 0" in { 
      jpyTogbpmoney.isLessThan(Money.of(CurrencyUnit.GBP, 1))
    }
  }                        
  
}


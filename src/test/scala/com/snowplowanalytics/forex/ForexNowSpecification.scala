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
import java.math.RoundingMode
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
  val fxWithBaseGBP = TestHelper.fxWithBaseGBP
  
  /**
  * trade 10000 USD to JPY at live exchange rate
  */
  val tradeInYenNow = fx.convert(10000).to(CurrencyUnit.JPY).now

  val jpyMoneyWithBaseUsd = tradeInYenNow.right.get
  
  "convert 10000 USD dollars to Yen now = [%s]".format(jpyMoneyWithBaseUsd) should {
     "be > 10000" in {
      jpyMoneyWithBaseUsd.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN))
     }
  }

  /**
  * GBP -> SGD with USD as base currency
  */
  val gbpToSgdWithBaseUsd = fx.rate(CurrencyUnit.GBP).to(CurrencyUnit.getInstance("SGD")).now

  val sgdMoneyWithBaseUsd = gbpToSgdWithBaseUsd.right.get 
  
  "GBP to SGD with base currency USD live exchange rate [%s]".format(sgdMoneyWithBaseUsd) should {
    "be greater than 1 SGD" in { 
      sgdMoneyWithBaseUsd.isGreaterThan(Money.of(CurrencyUnit.getInstance("SGD"), 1))
    }
  }                        
   
  /**
  * GBP -> SGD with GBP as base currency
  */
  val gbpToSgdWithBaseGbp = fxWithBaseGBP.rate.to(CurrencyUnit.getInstance("SGD")).now
  
  val sgdMoneyWithBaseGbp = gbpToSgdWithBaseUsd.right.get 
  
  "GBP to SGD with base currency GBP live exchange rate [%s]".format(sgdMoneyWithBaseGbp) should {
    "be greater than 1 SGD" in { 
      sgdMoneyWithBaseGbp.isGreaterThan(Money.of(CurrencyUnit.getInstance("SGD"), 1))
    }
  }        
}

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

class ForexEodSpec extends Specification { 
  val fx  = TestHelper.fx 
  val eodDate = new DateTime(2011, 3, 13, 0, 0)
  val gbpEodRate =  fx.rate.to(CurrencyUnit.GBP).eod(eodDate)
  "this conversion" should {
    "always result in a Right" in {
      gbpEodRate.isRight  
    }
  }
  val gbpmoney = gbpEodRate.right.get
  "USD to GBP eod rate [%s]".format(gbpmoney) should {
    "be > 0, historicalCache size = [%s]".format(fx.client.eodCache.size) in {
        gbpmoney.isPositive
    }
  }

  val tradeInYenHistorical = fx.convert(10000).to(CurrencyUnit.JPY).eod(eodDate) 
  "this conversion" should {
    "always result in a Right" in {
      tradeInYenHistorical.isRight  
    }
  }
  val yenmoney = tradeInYenHistorical.right.get
  "convert 10000 USD dollars to Yen in 2011 = [%s]".format(yenmoney) should {
    "be > 10000" in {
      yenmoney.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN))
    }
  }
  
}


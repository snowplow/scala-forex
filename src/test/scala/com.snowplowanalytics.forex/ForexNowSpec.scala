/*
 * Copyright (c) 2013-2018 Snowplow Analytics Ltd. All rights reserved.
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
import org.joda.money._
// TestHelpers
import TestHelpers._

/**
 * Testing method for getting the live exchange rate
 */
class ForexNowSpec extends Specification {

  /**
   * Trade 10000 USD to JPY at live exchange rate
   */
  val tradeInYenNow = fx.convert(10000).to(CurrencyUnit.JPY).now

  "convert 10000 USD dollars to Yen now" should {
    "be > 10000" in {
      tradeInYenNow
        .map(_ must beRight((m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN))))
        .unsafeRunSync()
    }
  }

  /**
   * GBP -> SGD with USD as base currency
   */
  val gbpToSgdWithBaseUsd = fx.rate(CurrencyUnit.GBP).to("SGD").now

  "GBP to SGD with base currency USD live exchange rate" should {
    "be greater than 1 SGD" in {
      gbpToSgdWithBaseUsd
        .map(_ must beRight((m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.of("SGD"), 1))))
        .unsafeRunSync()
    }
  }

  /**
   * GBP -> SGD with GBP as base currency
   */
  val gbpToSgdWithBaseGbp = fxWithBaseGBP.rate.to("SGD").now

  "GBP to SGD with base currency GBP live exchange rate" should {
    "be greater than 1 SGD" in {
      gbpToSgdWithBaseGbp
        .map(_ must beRight((m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.of("SGD"), 1))))
        .unsafeRunSync()
    }
  }

  /**
   * GBP with GBP as base currency
   */
  val gbpToGbpWithBaseGbp = fxWithBaseGBP.rate.to("GBP").now

  "Do not throw JodaTime exception on converting identical currencies" should {
    "be equal 1 GBP" in {
      gbpToGbpWithBaseGbp
        .map(_ must beRight((m: Money) => m.isEqual(Money.of(CurrencyUnit.of("GBP"), 1))))
        .unsafeRunSync()
    }
  }
}

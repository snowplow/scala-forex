/*
 * Copyright (c) 2013-2017 Snowplow Analytics Ltd. All rights reserved.
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
 * Testing method for getting the approximate exchange rate
 */
class ForexNowishSpec extends Specification {

  /**
   * CAD -> GBP with base currency USD
   */
  val cadOverGbpNowish = fx.rate("CAD").to(CurrencyUnit.GBP).nowish

  "CAD to GBP with USD as base currency returning near-live rate" should {
    "be smaller than 1 pound" in {
      cadOverGbpNowish
        .map(_ must beRight((m: Money) => m.isLessThan(Money.of(CurrencyUnit.GBP, 1))))
        .unsafeRunSync()
    }
  }

  /**
   * GBP -> JPY with base currency USD
   */
  val gbpToJpyWithBaseUsd = fx.rate(CurrencyUnit.GBP).to(CurrencyUnit.of("JPY")).nowish

  "GBP to JPY with USD as base currency returning near-live rate" should {
    "be greater than 1 Yen" in {
      gbpToJpyWithBaseUsd
        .map(_ must beRight((m: Money) =>
          m.isGreaterThan(BigMoney.of(CurrencyUnit.of("JPY"), 1).toMoney(RoundingMode.HALF_EVEN))))
        .unsafeRunSync()
    }
  }

  /**
   * GBP -> JPY with base currency GBP
   */
  val gbpToJpyWithBaseGbp = fxWithBaseGBP.rate.to(CurrencyUnit.of("JPY")).nowish

  "GBP to JPY with GBP as base currency returning near-live rate" should {
    "be greater than 1 Yen" in {
      gbpToJpyWithBaseGbp
        .map(_ must beRight((m: Money) =>
          m.isGreaterThan(BigMoney.of(CurrencyUnit.of("JPY"), 1).toMoney(RoundingMode.HALF_EVEN))))
        .unsafeRunSync()
    }
  }
}

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

import java.math.RoundingMode

import org.joda.money._
import org.specs2.mutable.Specification

import TestHelpers._

/**
 * Testing method for getting the approximate exchange rate
 */
class ForexNowishSpec extends Specification {

  /**
   * CAD -> GBP with base currency USD
   */
  val cadOverGbpNowish = fx.flatMap(_.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish)

  "CAD to GBP with USD as base currency returning near-live rate" should {
    "be smaller than 1 pound" in {
      cadOverGbpNowish
        .unsafeRunSync() must beRight((m: Money) => m.isLessThan(Money.of(CurrencyUnit.GBP, 1)))
    }
  }

  /**
   * GBP -> JPY with base currency USD
   */
  val gbpToJpyWithBaseUsd = fx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.JPY).nowish)

  "GBP to JPY with USD as base currency returning near-live rate" should {
    "be greater than 1 Yen" in {
      gbpToJpyWithBaseUsd
        .unsafeRunSync() must beRight(
        (m: Money) => m.isGreaterThan(BigMoney.of(CurrencyUnit.JPY, 1).toMoney(RoundingMode.HALF_EVEN)))
    }
  }

  /**
   * GBP -> JPY with base currency GBP
   */
  val gbpToJpyWithBaseGbp = fxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.JPY).nowish)

  "GBP to JPY with GBP as base currency returning near-live rate" should {
    "be greater than 1 Yen" in {
      gbpToJpyWithBaseGbp
        .unsafeRunSync() must beRight(
        (m: Money) => m.isGreaterThan(BigMoney.of(CurrencyUnit.of("JPY"), 1).toMoney(RoundingMode.HALF_EVEN)))
    }
  }
}

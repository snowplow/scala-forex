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

  val gbpmoney = cadOverGbpNowish.right.get

  "CAD to GBP with USD as base currency returning near-live rate [%s]".format(gbpmoney) should {
    "be smaller than 1 pound" in {
      gbpmoney.isLessThan(Money.of(CurrencyUnit.GBP, 1))
    }
  }

  /**
   * GBP -> JPY with base currency USD
   */
  val gbpToJpyWithBaseUsd = fx.rate(CurrencyUnit.GBP).to(CurrencyUnit.getInstance("JPY")).nowish

  val jpyMoneyWithBaseUsd = gbpToJpyWithBaseUsd.right.get

  "GBP to JPY with USD as base currency returning near-live rate [%s]".format(jpyMoneyWithBaseUsd) should {
    "be greater than 1 Yen" in {
      jpyMoneyWithBaseUsd.isGreaterThan(BigMoney.of(CurrencyUnit.getInstance("JPY"), 1).toMoney(RoundingMode.HALF_EVEN))
    }
  }

  /**
   * GBP -> JPY with base currency GBP
   */
  val gbpToJpyWithBaseGbp = fxWithBaseGBP.rate.to(CurrencyUnit.getInstance("JPY")).nowish

  val jpyMoneyWithBaseGbp = gbpToJpyWithBaseUsd.right.get

  "GBP to JPY with GBP as base currency returning near-live rate [%s]".format(jpyMoneyWithBaseGbp) should {
    "be greater than 1 Yen" in {
      jpyMoneyWithBaseGbp.isGreaterThan(BigMoney.of(CurrencyUnit.getInstance("JPY"), 1).toMoney(RoundingMode.HALF_EVEN))
    }
  }
}

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
import java.time.{ZoneId, ZonedDateTime}

// Joda
import org.joda.money.{CurrencyUnit, Money}

// Specs2
import org.specs2.mutable.Specification
// TestHelpers
import TestHelpers._

/**
 * Testing method for getting the latest end-of-day rate
 * prior to the datetime or the day after according to the user's setting
 */
class ForexAtSpec extends Specification {

  /**
   * GBP->CAD with USD as baseCurrency
   */
  val tradeDate = ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))

  val gbpToCadWithBaseUsd = fx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.CAD).at(tradeDate))

  val cadMoney = gbpToCadWithBaseUsd

  "GBP to CAD with USD as base currency returning latest eod rate" should {
    "be > 0" in {
      gbpToCadWithBaseUsd.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

  /**
   * GBP-> CAD with GBP as base currency
   */
  val gbpToCadWithBaseGbp = fxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.CAD).at(tradeDate))

  "GBP to CAD with GBP as base currency returning latest eod" should {
    "be > 0" in {
      gbpToCadWithBaseGbp.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

  /**
   * CNY -> GBP with USD as base currency
   */
  val cnyOverGbpHistorical = fx.flatMap(_.rate(CurrencyUnit.of("CNY")).to(CurrencyUnit.GBP).at(tradeDate))

  "CNY to GBP with USD as base currency returning latest eod rate" should {
    "be > 0" in {
      cnyOverGbpHistorical.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

}

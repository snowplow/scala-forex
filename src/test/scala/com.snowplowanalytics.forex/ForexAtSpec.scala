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

import java.time.{ZoneId, ZonedDateTime}

import cats.effect.IO
import org.joda.money.{CurrencyUnit, Money}
import org.specs2.mutable.Specification

/**
 * Testing method for getting the latest end-of-day rate
 * prior to the datetime or the day after according to the user's setting
 */
class ForexAtSpec extends Specification {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key = sys.env.getOrElse("OER_KEY", "")
  val fx  = Forex.getForex[IO](ForexConfig(key, DeveloperAccount))
  val fxWithBaseGBP =
    Forex.getForex[IO](ForexConfig(key, EnterpriseAccount, baseCurrency = CurrencyUnit.GBP))

  val tradeDate =
    ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))

  /** GBP->CAD with USD as baseCurrency */
  "GBP to CAD with USD as base currency returning latest eod rate" should {
    "be > 0" in {
      val gbpToCadWithBaseUsd =
        fx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.CAD).at(tradeDate))
      gbpToCadWithBaseUsd.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

  /** GBP-> CAD with GBP as base currency */
  "GBP to CAD with GBP as base currency returning latest eod" should {
    "be > 0" in {
      val gbpToCadWithBaseGbp = fxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.CAD).at(tradeDate))
      gbpToCadWithBaseGbp.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

  /** CNY -> GBP with USD as base currency */
  "CNY to GBP with USD as base currency returning latest eod rate" should {
    "be > 0" in {
      val cnyOverGbpHistorical =
        fx.flatMap(_.rate(CurrencyUnit.of("CNY")).to(CurrencyUnit.GBP).at(tradeDate))
      cnyOverGbpHistorical.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

}

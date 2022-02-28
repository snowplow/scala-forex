/*
 * Copyright (c) 2013-2022 Snowplow Analytics Ltd. All rights reserved.
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
import cats.effect.unsafe.implicits.global
import org.joda.money.{CurrencyUnit, Money}
import org.specs2.mutable.Specification

import model._

/**
  * Testing method for getting the latest end-of-day rate
  * prior to the datetime or the day after according to the user's setting
  */
class ForexAtSpec extends Specification {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key  = sys.env.getOrElse("OER_KEY", "")
  val ioFx = CreateForex[IO].create(ForexConfig(key, DeveloperAccount))
  val ioFxWithBaseGBP =
    CreateForex[IO].create(ForexConfig(key, EnterpriseAccount, baseCurrency = CurrencyUnit.GBP))

  val tradeDate =
    ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))

  /** GBP->CAD with USD as baseCurrency */
  "GBP to CAD with USD as base currency returning latest eod rate" should {
    "be > 0" in {
      val ioGbpToCadWithBaseUsd =
        ioFx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.CAD).at(tradeDate))
      ioGbpToCadWithBaseUsd.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

  /** GBP-> CAD with GBP as base currency */
  "GBP to CAD with GBP as base currency returning latest eod" should {
    "be > 0" in {
      val ioGbpToCadWithBaseGbp = ioFxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.CAD).at(tradeDate))
      ioGbpToCadWithBaseGbp.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

  /** CNY -> GBP with USD as base currency */
  "CNY to GBP with USD as base currency returning latest eod rate" should {
    "be > 0" in {
      val ioCnyOverGbpHistorical =
        ioFx.flatMap(_.rate(CurrencyUnit.of("CNY")).to(CurrencyUnit.GBP).at(tradeDate))
      ioCnyOverGbpHistorical.unsafeRunSync() must beRight((m: Money) => m.isPositive)
    }
  }

}

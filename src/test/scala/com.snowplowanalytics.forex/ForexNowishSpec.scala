/*
 * Copyright (c) 2013-2019 Snowplow Analytics Ltd. All rights reserved.
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

import cats.effect.IO
import org.joda.money._
import org.specs2.mutable.Specification

import model._

/** Testing method for getting the approximate exchange rate */
class ForexNowishSpec extends Specification {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key = sys.env.getOrElse("OER_KEY", "")
  val ioFx  = Forex.getForex[IO](ForexConfig(key, DeveloperAccount))
  val ioFxWithBaseGBP =
    Forex.getForex[IO](ForexConfig(key, EnterpriseAccount, baseCurrency = CurrencyUnit.GBP))
  val evalFx  = Forex.unsafeGetForex(ForexConfig(key, DeveloperAccount))
  val evalFxWithBaseGBP =
    Forex.unsafeGetForex(ForexConfig(key, EnterpriseAccount, baseCurrency = CurrencyUnit.GBP))

  /** CAD -> GBP with base currency USD */
  "CAD to GBP with USD as base currency returning near-live rate" should {
    "be smaller than 1 pound" in {
      val ioCadOverGbpNowish = ioFx.flatMap(_.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish)
      ioCadOverGbpNowish
        .unsafeRunSync() must beRight((m: Money) => m.isLessThan(Money.of(CurrencyUnit.GBP, 1)))
      val evalCadOverGbpNowish =
        evalFx.flatMap(_.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish)
      evalCadOverGbpNowish
        .value must beRight((m: Money) => m.isLessThan(Money.of(CurrencyUnit.GBP, 1)))
    }
  }

  /** GBP -> JPY with base currency USD */
  "GBP to JPY with USD as base currency returning near-live rate" should {
    "be greater than 1 Yen" in {
      val ioGbpToJpyWithBaseUsd = ioFx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.JPY).nowish)
      ioGbpToJpyWithBaseUsd.unsafeRunSync() must beRight((m: Money) =>
        m.isGreaterThan(BigMoney.of(CurrencyUnit.JPY, 1).toMoney(RoundingMode.HALF_EVEN)))
      val evalGbpToJpyWithBaseUsd =
        evalFx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.JPY).nowish)
      evalGbpToJpyWithBaseUsd.value must beRight((m: Money) =>
        m.isGreaterThan(BigMoney.of(CurrencyUnit.JPY, 1).toMoney(RoundingMode.HALF_EVEN)))
    }
  }

  /** GBP -> JPY with base currency GBP */
  "GBP to JPY with GBP as base currency returning near-live rate" should {
    "be greater than 1 Yen" in {
      val ioGbpToJpyWithBaseGbp = ioFxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.JPY).nowish)
      ioGbpToJpyWithBaseGbp.unsafeRunSync() must beRight((m: Money) =>
        m.isGreaterThan(BigMoney.of(CurrencyUnit.of("JPY"), 1).toMoney(RoundingMode.HALF_EVEN)))
      val evalGbpToJpyWithBaseGbp = evalFxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.JPY).nowish)
      evalGbpToJpyWithBaseGbp.value must beRight((m: Money) =>
        m.isGreaterThan(BigMoney.of(CurrencyUnit.of("JPY"), 1).toMoney(RoundingMode.HALF_EVEN)))
    }
  }
}

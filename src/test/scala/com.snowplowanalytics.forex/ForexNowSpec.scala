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

import cats.Eval
import cats.effect.IO
import org.joda.money._
import org.specs2.mutable.Specification

import model._

/** Testing method for getting the live exchange rate */
class ForexNowSpec extends Specification {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key  = sys.env.getOrElse("OER_KEY", "")
  val ioFx = CreateForex[IO].create(ForexConfig(key, DeveloperAccount))
  val ioFxWithBaseGBP =
    CreateForex[IO].create(ForexConfig(key, EnterpriseAccount, baseCurrency = CurrencyUnit.GBP))
  val evalFx = CreateForex[Eval].create(ForexConfig(key, DeveloperAccount))
  val evalFxWithBaseGBP =
    CreateForex[Eval].create(ForexConfig(key, EnterpriseAccount, baseCurrency = CurrencyUnit.GBP))

  /** Trade 10000 USD to JPY at live exchange rate */
  "convert 10000 USD dollars to Yen now" should {
    "be > 10000" in {
      val ioTradeInYenNow = ioFx.flatMap(_.convert(10000).to(CurrencyUnit.JPY).now)
      ioTradeInYenNow.unsafeRunSync() must beRight(
        (m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN)))
      val evalTradeInYenNow = evalFx.flatMap(_.convert(10000).to(CurrencyUnit.JPY).now)
      evalTradeInYenNow.value must beRight(
        (m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.JPY, 10000, RoundingMode.HALF_EVEN)))
    }
  }

  /** GBP -> SGD with USD as base currency */
  "GBP to SGD with base currency USD live exchange rate" should {
    "be greater than 1 SGD" in {
      val ioGbpToSgdWithBaseUsd =
        ioFx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.of("SGD")).now)
      ioGbpToSgdWithBaseUsd.unsafeRunSync() must beRight(
        (m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.of("SGD"), 1)))
      val evalGbpToSgdWithBaseUsd =
        evalFx.flatMap(_.rate(CurrencyUnit.GBP).to(CurrencyUnit.of("SGD")).now)
      evalGbpToSgdWithBaseUsd.value must beRight((m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.of("SGD"), 1)))
    }
  }

  /** GBP -> SGD with GBP as base currency */
  "GBP to SGD with base currency GBP live exchange rate" should {
    "be greater than 1 SGD" in {
      val ioGbpToSgdWithBaseGbp = ioFxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.of("SGD")).now)
      ioGbpToSgdWithBaseGbp.unsafeRunSync() must beRight(
        (m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.of("SGD"), 1)))
      val evalGbpToSgdWithBaseGbp = evalFxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.of("SGD")).now)
      evalGbpToSgdWithBaseGbp.value must beRight((m: Money) => m.isGreaterThan(Money.of(CurrencyUnit.of("SGD"), 1)))
    }
  }

  /** GBP with GBP as base currency */
  "Do not throw JodaTime exception on converting identical currencies" should {
    "be equal 1 GBP" in {
      val ioGbpToGbpWithBaseGbp = ioFxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.GBP).now)
      ioGbpToGbpWithBaseGbp.unsafeRunSync() must beRight((m: Money) => m.isEqual(Money.of(CurrencyUnit.of("GBP"), 1)))
      val evalGbpToGbpWithBaseGbp = evalFxWithBaseGBP.flatMap(_.rate.to(CurrencyUnit.GBP).now)
      evalGbpToGbpWithBaseGbp.value must beRight((m: Money) => m.isEqual(Money.of(CurrencyUnit.of("GBP"), 1)))
    }
  }
}

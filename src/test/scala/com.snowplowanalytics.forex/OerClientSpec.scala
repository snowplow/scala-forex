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

import java.math.BigDecimal
import java.time.ZonedDateTime

import cats.effect.IO
import org.joda.money.CurrencyUnit
import org.specs2.mutable.Specification

import model._

/** Testing methods for Open exchange rate client */
class OerClientSpec extends Specification {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key = sys.env.getOrElse("OER_KEY", "")
  val ioFx = Forex.getForex[IO](ForexConfig(key, DeveloperAccount))
  val evalFx = Forex.unsafeGetForex(ForexConfig(key, DeveloperAccount))

  "live currency value for USD" should {
    "always equal to 1" in {
      ioFx.map(_.client)
        .flatMap(_.getLiveCurrencyValue(CurrencyUnit.USD))
        .unsafeRunSync() must beRight(new BigDecimal(1))
      evalFx.map(_.client)
        .flatMap(_.getLiveCurrencyValue(CurrencyUnit.USD))
        .value must beRight(new BigDecimal(1))
    }
  }

  "live currency value for GBP" should {
    "be less than 1" in {
      val ioGbpLiveRate = ioFx.flatMap(_.client.getLiveCurrencyValue(CurrencyUnit.GBP))
      ioGbpLiveRate.unsafeRunSync() must beRight((d: BigDecimal) => d.doubleValue < 1)
      val evalGbpLiveRate = evalFx.flatMap(_.client.getLiveCurrencyValue(CurrencyUnit.GBP))
      evalGbpLiveRate.value must beRight((d: BigDecimal) => d.doubleValue < 1)
    }
  }

  "historical currency value for USD on 01/01/2008" should {
    "always equal to 1 as well" in {
      val date = ZonedDateTime.parse("2008-01-01T01:01:01.123+09:00")
      ioFx.flatMap(_.client.getHistoricalCurrencyValue(CurrencyUnit.USD, date))
        .unsafeRunSync() must beRight(new BigDecimal(1))
      evalFx.flatMap(_.client.getHistoricalCurrencyValue(CurrencyUnit.USD, date))
        .value must beRight(new BigDecimal(1))
    }
  }
}

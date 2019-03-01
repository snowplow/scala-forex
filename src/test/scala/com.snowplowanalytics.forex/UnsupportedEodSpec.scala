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
import org.joda.money.CurrencyUnit
import org.specs2.mutable.Specification

import errors._
import model._

/**
 *  Testing for exceptions caused by invalid dates
 */
class UnsupportedEodSpec extends Specification {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key = sys.env.getOrElse("OER_KEY", "")
  val fx  = Forex.getForex[IO](ForexConfig(key, DeveloperAccount))

  "An end-of-date lookup in 1900" should {
    "throw an exception" in {

      /**
       * 1900 is earlier than 1990 which is the earliest available date for looking up exchange
       * rates
       */
      val date1900 = ZonedDateTime.of(1900, 3, 13, 0, 0, 0, 0, ZoneId.systemDefault)
      fx.flatMap(_.rate.to(CurrencyUnit.GBP).eod(date1900))
        .unsafeRunSync() must beLike {
        case Left(OerResponseError(_, ResourcesNotAvailable)) => ok
      }
    }
  }

  "An end-of-date lookup in 2030" should {
    "throw an exception" in {

      /** 2030 is in the future so it won't be available either */
      val date2030 = ZonedDateTime.of(2030, 3, 13, 0, 0, 0, 0, ZoneId.systemDefault)
      fx.flatMap(_.rate.to(CurrencyUnit.GBP).eod(date2030))
        .unsafeRunSync() must beLike {
        case Left(OerResponseError(_, ResourcesNotAvailable)) => ok
      }
    }
  }
}

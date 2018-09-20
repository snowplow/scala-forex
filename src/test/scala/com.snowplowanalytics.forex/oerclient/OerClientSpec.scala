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
package oerclient

// Java
import java.math.BigDecimal
import java.time.ZonedDateTime

// Specs2
import org.specs2.mutable.Specification

import TestHelpers._

/** Testing methods for Open exchange rate client */
class OerClientSpec extends Specification {

  "live currency value for USD" should {
    "always equal to 1" in {
      fx.client
        .getLiveCurrencyValue("USD")
        .map(_ must beRight(new BigDecimal(1)))
        .unsafeRunSync()
    }
  }

  val gbpLiveRate = fx.client.getLiveCurrencyValue("GBP")
  "live currency value for GBP" should {
    "be less than 1" in {
      gbpLiveRate
        .map(_ must beRight((d: BigDecimal) => d.doubleValue() < 1))
        .unsafeRunSync()
    }
  }

  val date = ZonedDateTime.parse("2008-01-01T01:01:01.123+09:00")
  "historical currency value for USD on 01/01/2008" should {
    "always equal to 1 as well" in {
      fx.client
        .getHistoricalCurrencyValue("USD", date)
        .map(_ must beRight(new BigDecimal(1)))
        .unsafeRunSync()
    }
  }
}

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
import java.time.{ZoneId, ZonedDateTime}

// Specs2
import org.specs2.mutable.Specification
// TestHelpers
import TestHelpers._
// oerclient
import oerclient.OerResponseError
import oerclient.ResourcesNotAvailable

/**
 *  Testing for exceptions caused by invalid dates
 */
class UnsupportedEodSpec extends Specification {

  "An end-of-date lookup in 1900" should {
    "throw an exception" in {

      /**
       * 1900 is earlier than 1990 which is the earliest available date for looking up exchange rates
       */
      val date1900   = ZonedDateTime.of(1900, 3, 13, 0, 0, 0, 0, ZoneId.systemDefault)
      val rateIn1900 = fx.rate.to("GBP").eod(date1900)
      rateIn1900 must beLike {
        case Left(OerResponseError(_, ResourcesNotAvailable)) => ok
      }
    }
  }

  "An end-of-date lookup in 2020" should {
    "throw an exception" in {

      /**
       * 2020 is in the future so it won't be available either
       */
      val date2020   = ZonedDateTime.of(2020, 3, 13, 0, 0, 0, 0, ZoneId.systemDefault)
      val rateIn2020 = fx.rate.to("GBP").eod(date2020)
      rateIn2020 must beLike {
        case Left(OerResponseError(_, ResourcesNotAvailable)) => ok
      }
    }
  }
}

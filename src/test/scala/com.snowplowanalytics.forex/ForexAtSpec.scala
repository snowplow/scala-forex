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

/**
 * Testing method for getting the latest end-of-day rate
 * prior to the datetime or the day after according to the user's setting
 */
class ForexAtSpec extends Specification {

  /**
   * GBP->CAD with USD as baseCurrency
   */
  val tradeDate = ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))

  val gbpToCadWithBaseUsd = fx.rate("GBP").to("CAD").at(tradeDate)

  val cadMoney = gbpToCadWithBaseUsd.right.get

  "GBP to CAD with USD as base currency returning latest eod rate [%s]".format(cadMoney) should {
    "be > 0" in {
      cadMoney.isPositive
    }
  }

  /**
   * GBP-> CAD with GBP as base currency
   */
  val gbpToCadWithBaseGbp = fxWithBaseGBP.rate.to("CAD").at(tradeDate)

  val cadMoneyWithBaseGbp = gbpToCadWithBaseGbp.right.get

  "GBP to CAD with GBP as base currency returning latest eod rate [%s]".format(cadMoneyWithBaseGbp) should {
    "be > 0" in {
      cadMoneyWithBaseGbp.isPositive
    }
  }

  /**
   * CNY -> GBP with USD as base currency
   */
  val cnyOverGbpHistorical = fx.rate("CNY").to("GBP").at(tradeDate)

  val cnyTogbpmoney = cnyOverGbpHistorical.right.get

  "CNY to GBP with USD as base currency returning latest eod rate [%s]".format(cnyTogbpmoney) should {
    "be > 0" in {
      cnyTogbpmoney.isPositive
    }
  }

}

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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import cats.effect.IO
import org.joda.money.{CurrencyUnit, Money}
import org.specs2.mutable.Specification
import org.specs2.matcher.DataTables

import model._

/**
 * Testing method for getting the end-of-date exchange rate
 * since historical forex rate is fixed, the actual look up result should be
 * the same as the value in the table
 */
class ForexEodSpec extends Specification with DataTables {

  val key = sys.env.getOrElse("OER_KEY", "")
  val fx  = Forex.getForex[IO](ForexConfig(key, DeveloperAccount))

  override def is =
    skipAllIf(sys.env.get("OER_KEY").isEmpty) ^
      "end-of-date lookup tests: forex rate between two currencies for a specific date is always the same" ! e1

  // Table values obtained from OER API
  def e1 =
    "SOURCE CURRENCY"  || "TARGET CURRENCY"      | "DATE"                      | "EXPECTED OUTPUT" |
      CurrencyUnit.USD !! CurrencyUnit.GBP       ! "2011-03-13T13:12:01+00:00" ! "0.62" |
      CurrencyUnit.USD !! CurrencyUnit.of("AED") ! "2011-03-13T01:13:04+00:00" ! "3.67" |
      CurrencyUnit.USD !! CurrencyUnit.CAD       ! "2011-03-13T22:13:01+00:00" ! "0.98" |
      CurrencyUnit.GBP !! CurrencyUnit.USD       ! "2011-03-13T11:45:34+00:00" ! "1.60" |
      CurrencyUnit.GBP !! CurrencyUnit.of("SGD") ! "2008-03-13T00:01:01+00:00" ! "2.80" |> {
      (fromCurr, toCurr, date, exp) =>
        fx.flatMap(_.rate(fromCurr).to(toCurr).eod(ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
          .unsafeRunSync() must beRight((m: Money) => m.getAmount.toString mustEqual exp)
    }
}

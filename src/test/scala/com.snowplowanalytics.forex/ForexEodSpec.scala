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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Joda-Money
import org.joda.money.Money

// Specs2
import org.specs2.mutable.Specification
import org.specs2.matcher.DataTables
// TestHelpers
import TestHelpers._

/**
 * Testing method for getting the end-of-date exchange rate
 * since historical forex rate is fixed, the actual look up result should be
 * the same as the value in the table
 */
class ForexEodSpec extends Specification with DataTables {

  override def is =
    "end-of-date lookup tests: forex rate between two currencies for a specific date is always the same" ! e1

  // Table values obtained from OER API
  def e1 =
    "SOURCE CURRENCY" || "TARGET CURRENCY" | "DATE"                      | "EXPECTED OUTPUT" |
      "USD"           !! "GBP"             ! "2011-03-13T13:12:01+00:00" ! "0.62" |
      "USD"           !! "AED"             ! "2011-03-13T01:13:04+00:00" ! "3.67" |
      "USD"           !! "CAD"             ! "2011-03-13T22:13:01+00:00" ! "0.98" |
      "GBP"           !! "USD"             ! "2011-03-13T11:45:34+00:00" ! "1.60" |
      "GBP"           !! "SGD"             ! "2008-03-13T00:01:01+00:00" ! "2.80" |> { (fromCurr, toCurr, date, exp) =>
      fx.rate(fromCurr)
        .to(toCurr)
        .eod(ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .map(_ must beRight((m: Money) => m.getAmount.toString mustEqual exp))
        .unsafeRunSync()
    }
}

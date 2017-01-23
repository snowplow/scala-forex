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

// Specs2
import org.specs2.mutable.Specification
import org.specs2.matcher.DataTables
// Joda 
import org.joda.time._
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
    "SOURCE CURRENCY"   || "TARGET CURRENCY"   | "DATE"        | "EXPECTED OUTPUT"  |
    "USD"               !! "GBP"               ! "2011-03-13"  ! "0.62"             |
    "USD"               !! "AED"               ! "2011-03-13"  ! "3.67"             |
    "USD"               !! "CAD"               ! "2011-03-13"  ! "0.98"             |
    "GBP"               !! "USD"               ! "2011-03-13"  ! "1.60"             |
    "GBP"               !! "SGD"               ! "2008-03-13"  ! "2.80"             |> {
      (fromCurr, toCurr, date, exp) =>
        fx.rate(fromCurr).to(toCurr).eod(DateTime.parse(date)).right.get.getAmount.toString must_== exp  
    }
}

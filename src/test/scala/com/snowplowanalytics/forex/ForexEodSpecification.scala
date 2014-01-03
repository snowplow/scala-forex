/* 
 * Copyright (c) 2013-2014 Snowplow Analytics Ltd. All rights reserved.
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
import java.math.BigDecimal
import java.math.RoundingMode
// Scala
import scala.collection.JavaConversions._
// Specs2
import org.specs2.mutable.Specification
import org.specs2.matcher.DataTables
// Joda 
import org.joda.time._
import org.joda.money._
import com.snowplowanalytics.forex.Forex
import com.snowplowanalytics.forex.ForexConfig
/**
* Testing method for getting the end-of-date exchange rate
*/
class ForexEodSpecification extends Specification with DataTables { 
  val fx = new Forex(new ForexConfig(System.getenv("SBT_OPTS").split("=")(1), false))
  

  override def is = 
    "forex rate between two currencies for a specific date is always the same" ! e1

  def e1 = 
    "SOURCE CURRENCY"   || "TARGET CURRENCY"   | "DATE"        | "EXPECTED OUTPUT"  |
    "USD"               !! "GBP"               ! "2011-03-13"  ! "0.62"             |
    "USD"               !! "AED"               ! "2011-03-13"  ! "3.67"             |
    "USD"               !! "CAD"               ! "2011-03-13"  ! "0.98"             |
    "GBP"               !! "USD"               ! "2011-03-13"  ! "1.60"             |
    "GBP"               !! "SGD"               ! "2008-03-13"  ! "2.80"             |> {
      (fromCurr, toCurr, date, exp) =>
        fx.rate(CurrencyUnit.getInstance(fromCurr))
            .to(CurrencyUnit.getInstance(toCurr))
              .eod(DateTime.parse(date)).right.get.getAmount.toString must_== exp
    }
}


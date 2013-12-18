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
package com.snowplowanalytics.oerforex

// Java
import java.math.BigDecimal

// Java OER
import org.openexchangerates.oerjava.OpenExchangeRates
import org.openexchangerates.oerjava.Currency

// Scala
import scala.collection.JavaConversions._

// Specs2
import org.specs2.mutable.Specification

// Joda time
import org.joda.time._

class JavaOerTest extends Specification { 

  //run "sbt -Dkey=<<Key>> test" in the command line
  //e.g.  if your key is 123, then run "sbt -Dkey=123 test" 
  val oer = OpenExchangeRates.getClient(System.getProperty("key")) 

  for (entry <- oer.getLatest.entrySet) {
    "live exchange rate for currency [%s]".format(entry.getKey) should { 
      "be greater than 0" in {
        entry.getValue must beGreaterThan (new BigDecimal(0))
      }
    }
  }

  

  val cal = DateTime.parse("2008-01-01T01:01:01.123+0900").toGregorianCalendar

  for (entry <- oer.getHistorical(cal).entrySet) {
        "exchange rate for currency [%s] on 01/01/2008".format(entry.getKey) should {
          "be greater than 0" in {
        entry.getValue must beGreaterThan (new BigDecimal(0))
      }
    }
  }

  "live currency value for USD" should { 
    "always equal to 1" in {
      oer.getCurrencyValue(Currency.USD) must_== (new BigDecimal(1))
    }
  }

  "historical currency value for USD on 01/01/2008" should {
    "always equal to 1 as well" in {
      oer.getHistoricalCurrencyValue(Currency.USD, cal) must_== (new BigDecimal(1))
    }
  }
}




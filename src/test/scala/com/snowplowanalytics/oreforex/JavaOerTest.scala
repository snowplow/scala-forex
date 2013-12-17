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

// Java
import java.math.BigDecimal

// Java OER
import org.openexchangerates.oerjava.OpenExchangeRates

// Scala
import scala.collection.JavaConversions._

// Specs2
import org.specs2.mutable.Specification

class JavaOerTest extends Specification { /*def is =

  "This is a specification to check that the underlying Java Oer client is working correctly" ^
                                                                                             p^
  "all live exchange rates should be greater than 0"                                          ! e1^
                                                                                              end */


  // TODO: figure out way of fetching API key from environment variable or similar so that we don't
  // have to keep adding and removing the key to avoid accidentally leaking it on GitHub
  // TODO: explore ways of starting SBT with a Java environment variable
  val ore = OpenExchangeRates.getClient("<<ADD IN YOUR KEY>>") // Do not commit with key added!!

  for (entry <- ore.getLatest.entrySet) {
    "live exchange rate for currency [%s]".format(entry.getKey) should { // TODO: put currency code in here
      "be greater than 0" in {
        entry.getValue must beGreaterThan (new BigDecimal(0))
      }
    }
  }

  // TODO: add more tests as per https://github.com/snowplow/snowplow/blob/master/3-enrich/hadoop-etl/src/test/scala/utils/MapTransformerTest.scala
}
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

// Java OER
import org.openexchangerates.oerjava.OpenExchangeRates
import org.openexchangerates.oerjava.Currency

// Scala
import scala.collection.JavaConversions._

// Specs2
import org.specs2.mutable.Specification

// Joda time
import org.joda.time._

class ScalaOerTest extends Specification { 
   val oer = OpenExchangeRates.getClient(System.getProperty("key")) 

   val fxb = ForexBuilder(System.getProperty("key"))

   val fx = Forex(fxb)

   "USD/GBP live rate " should {
     "be greater than 0" in { 
      fx.rate.to(Currency.GBP).now must beGreaterThan (new BigDecimal(0))
     }
  }
}
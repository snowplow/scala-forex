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

// Specs2
import org.specs2.mutable.Specification
// Joda 
import org.joda.time._

/**
 *  Testing for exceptions caused by invalid dates
 */
class UnsupportedEodSpec extends Specification { 
  val fx  = TestHelper.fx 
  
  /**
   * 1900 is earlier than 1990 which is the earliest available date for looking up exchange rates  
   */
  val rateIn1900 = fx.rate.to("GBP").eod(new DateTime(1900, 3, 13, 0, 0))
  
  "An end-of-date lookup in 1900" should {
    "throw an exception" in {
      rateIn1900.isLeft
    }
  }

 /**
  * 2020 is in the future so it won't be available either
  */
  val rateIn2020 = fx.rate.to("GBP").eod(new DateTime(2020, 3, 13, 0, 0))
  
  "An end-of-date lookup in 2020" should {
    "throw an exception" in {
      rateIn2020.isLeft
    }
  }
}

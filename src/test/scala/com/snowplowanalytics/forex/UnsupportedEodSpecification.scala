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
// Joda 
import org.joda.money._
import org.joda.time._

/**
*  testing for exceptions caused by invalid dates
*/
class UnsupportedEodSpecification extends Specification { 
  // run 'export SBT_OPTS=-Dforex.key=[key]' in command line before running tests
  val fx  = TestHelper.fx 
  
  "a historical lookup in 1900" should {
	"throw an exception" in {
		 def eodException: Either[String, Money] = {
		 	val eodDate = new DateTime(1900, 3, 13, 0, 0)
		 	fx.rate.to(CurrencyUnit.GBP).eod(eodDate)
		 }
	 		eodException.isLeft
	 }
  }

}
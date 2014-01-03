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

 
package com.snowplowanalytics.forex.oerclient

// Java
import java.math.BigDecimal
// Scala
import scala.collection.JavaConversions._
// Specs2
import org.specs2.mutable.Specification
// Joda 
import org.joda.time._
import org.joda.money._

import com.snowplowanalytics.forex.Forex
import com.snowplowanalytics.forex.ForexConfig
/**
* Testing methods for Open exchange rate client 
*/
class OerClientSpecification extends Specification { 
  val fx = new Forex(new ForexConfig(System.getenv("SBT_OPTS").split("=")(1), false))
  "live currency value for USD" should { 
    "always equal to 1" in {
      fx.client.getCurrencyValue(CurrencyUnit.USD) must_== (new BigDecimal(1))
    }
  }

  val gbpLiveRate =  fx.client.getCurrencyValue(CurrencyUnit.GBP)
  "live currency value for GBP [%s]".format(gbpLiveRate) should {
    "be less than 1" in {
      gbpLiveRate must be < (new BigDecimal(1))
    }
  }

  val date = DateTime.parse("2008-01-01T01:01:01.123+0900")
  "historical currency value for USD on 01/01/2008" should {
    "always equal to 1 as well" in {
      fx.client.getHistoricalCurrencyValue(CurrencyUnit.USD, date) must_== Right((new BigDecimal(1)))
    }
  }
}


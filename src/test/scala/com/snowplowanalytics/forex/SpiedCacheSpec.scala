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
// Mockito
import org.specs2.mock.Mockito
// Joda 
import org.joda.time._
// LRUCache
import com.twitter.util.LruMap
// Java
import java.lang.Thread
import java.math.BigDecimal
// TestHelpers
import TestHelpers._

/**
 * Testing cache behaviours
 */
class SpiedCacheSpec extends Specification with Mockito{
  val spiedNowishCache = spy(new LruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize)) 
  val spiedEodCache    = spy(new LruMap[EodCacheKey, EodCacheValue](config.eodCacheSize))
  val spiedFx = Forex.getForex(config, oerConfig, Some(spiedNowishCache), Some(spiedEodCache)) 
  val spiedFxWith5NowishSecs = Forex.getForex(fxConfigWith5NowishSecs, oerConfig, Some(spiedNowishCache), Some(spiedEodCache)) 

  /**
   * nowish cache with 5-sec memory
   */
  "A lookup of CAD->GBP within memory time limit" should {
    "return the value stored in the nowish cache" in {
      // call nowish, update the cache with key("CAD","GBP") and corresponding value
      spiedFxWith5NowishSecs.rate("CAD").to("GBP").nowish
      // get the value from the first HTPP request
      val valueFromFirstHttpRequest = spiedNowishCache.get(("CAD", "GBP")).get
      // call nowish within 5 secs will get the value from the cache which is the same as valueFromFirstHttpRequest
      spiedFxWith5NowishSecs.rate("CAD").to("GBP").nowish
      // nowish will get the value from cache
      spiedNowishCache must haveValue(valueFromFirstHttpRequest)
      // pause for 6 secs
      Thread.sleep(6000)
      // nowish will get the value over HTTP request, which will replace the previous value in the cache
      spiedFxWith5NowishSecs.rate("CAD").to("GBP").nowish
      // value will be different from previous value -
      // even if the monetary value is the same, the
      // timestamp will be different
      "A second time lookup of CAD->GBP after the memory time" + 
        "should overwrite the value in the nowish cache" in {
        spiedNowishCache must haveValue(valueFromFirstHttpRequest).not
      }
    }
   } 
  

  /**
   * CAD -> GBP with base currency USD on 13-03-2011
   * The eod lookup will call get method on eod cache
   * after the call, the key will be stored in the cache 
   */
  val date = new DateTime(2011, 3, 13, 0, 0)
  
  spiedFx.rate("CAD").to("GBP").eod(date)

  "Eod query on CAD->GBP" should {
    "call get method on eod cache" in {
      there was one(spiedEodCache).get(("CAD", "GBP", date)) 
      "Eod cache have (CAD, GBP) entry after the query" in {
        spiedEodCache must haveKey(("CAD", "GBP", date)) 
      }
    }
  }



}

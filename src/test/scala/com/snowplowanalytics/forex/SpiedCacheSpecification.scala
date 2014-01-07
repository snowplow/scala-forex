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
import org.specs2.matcher._
import org.specs2.mock.Mockito
// Joda 
import org.joda.time._
import org.joda.money._
// LRUCache
import com.twitter.util.LruMap

/**
* Testing cache behaviours
*/
class SpiedCacheSpecification extends Specification with Mockito{
  val spiedNowishCache = spy(new LruMap[NowishCacheKey, NowishCacheValue](TestHelper.config.nowishCacheSize)) 
  val spiedEodCache    = spy(new LruMap[EodCacheKey, EodCacheValue](TestHelper.config.eodCacheSize))
  val spiedFx = Forex.getSpiedForex(TestHelper.config, TestHelper.oerConfig, Some(spiedNowishCache), Some(spiedEodCache)) 
  
  /**
  * CAD -> GBP with base currency USD
  * The nowish lookup will call get method on nowish cache 
  * after the call, the key will be stored in the cache 
  */
  spiedFx.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish

  there was one(spiedNowishCache).get(((CurrencyUnit.CAD), (CurrencyUnit.GBP)))          
  
  spiedNowishCache must haveKey(((CurrencyUnit.CAD), (CurrencyUnit.GBP)))


  /**
  * CAD -> GBP with base currency USD on 13-03-2011
  * The eod lookup will call get method on eod cache
  * after the call, the key will be stored in the cache 
  */
  val date = new DateTime(2011, 3, 13, 0, 0)
  
  spiedFx.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).eod(date)

  there was one(spiedEodCache).get(((CurrencyUnit.CAD), (CurrencyUnit.GBP), date)) 

  spiedEodCache must haveKey(((CurrencyUnit.CAD), (CurrencyUnit.GBP), date)) 
}
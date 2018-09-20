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

// Java
import java.lang.Thread
import java.math.BigDecimal
import java.time.{ZoneId, ZonedDateTime}

// cats
import cats.effect.IO
import cats.syntax.apply._

// LruMap
import com.snowplowanalytics.lrumap.LruMap

// Specs2
import org.specs2.mutable.Specification
// Mockito
import org.specs2.mock.Mockito
// TestHelpers
import TestHelpers._

/**
 * Testing cache behaviours
 */
class SpiedCacheSpec extends Specification with Mockito {
  val spiedNowishCache = spy(
    LruMap.create[IO, NowishCacheKey, NowishCacheValue](config.nowishCacheSize).unsafeRunSync())
  val spiedEodCache = spy(LruMap.create[IO, EodCacheKey, EodCacheValue](config.eodCacheSize).unsafeRunSync())
  val spiedFx       = Forex.getForex[IO](config, oerConfig, Some(spiedNowishCache), Some(spiedEodCache))
  val spiedFxWith5NowishSecs =
    Forex.getForex[IO](fxConfigWith5NowishSecs, oerConfig, Some(spiedNowishCache), Some(spiedEodCache))

  /**
   * nowish cache with 5-sec memory
   */
  "A lookup of CAD->GBP within memory time limit" should {
    "return the value stored in the nowish cache and be overwritten after configured time by a new request" in {
      val action = for {
        // call nowish, update the cache with key("CAD","GBP") and corresponding value
        _ <- spiedFxWith5NowishSecs.rate("CAD").to("GBP").nowish
        // get the value from the first HTPP request
        valueFromFirstHttpRequest <- spiedNowishCache.get(("CAD", "GBP"))
        // call nowish within 5 secs will get the value from the cache which is the same as valueFromFirstHttpRequest
        _ <- spiedFxWith5NowishSecs.rate("CAD").to("GBP").nowish

        valueFromCache <- spiedNowishCache.get(("CAD", "GBP"))
        test1 = valueFromCache must be equalTo valueFromFirstHttpRequest

        _ <- IO(Thread.sleep(6000))
        // nowish will get the value over HTTP request, which will replace the previous value in the cache
        _ <- spiedFxWith5NowishSecs.rate("CAD").to("GBP").nowish

        // value will be different from previous value -
        // even if the monetary value is the same, the
        // timestamp will be different
        newValueFromCache <- spiedNowishCache.get(("CAD", "GBP"))
        test2 = newValueFromCache mustNotEqual valueFromFirstHttpRequest
      } yield test1 and test2

      action.unsafeRunSync()
    }
  }

  /**
   * CAD -> GBP with base currency USD on 13-03-2011
   * The eod lookup will call get method on eod cache
   * after the call, the key will be stored in the cache
   */
  val date = ZonedDateTime.of(2011, 3, 13, 0, 0, 0, 0, ZoneId.systemDefault)

  "Eod query on CAD->GBP" should {
    "call get method on eod cache and the cache should have (CAD, GBP) entry after" in {
      spiedFx
        .rate("CAD")
        .to("GBP")
        .eod(date)
        .productR(spiedEodCache.get(("CAD", "GBP", date)))
        .map { valueFromCache =>
          there was two(spiedEodCache).get(("CAD", "GBP", date)) and (valueFromCache must beSome)
        }
        .unsafeRunSync()
    }
  }

}

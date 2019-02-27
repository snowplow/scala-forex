/*
 * Copyright (c) 2013-2018 Snowplow Analytics Ltd. All rights reserved.
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

import java.time.{ZoneId, ZonedDateTime}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import cats.effect.IO
import cats.syntax.apply._
import org.joda.money.CurrencyUnit
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import com.snowplowanalytics.lrumap.LruMap
import TestHelpers._

/**
 * Testing cache behaviours
 */
class SpiedCacheSpec extends Specification with Mockito {
  val spiedNowishCache = spy(
    LruMap.create[IO, NowishCacheKey, NowishCacheValue](config.nowishCacheSize).unsafeRunSync())
  val spiedEodCache = spy(LruMap.create[IO, EodCacheKey, EodCacheValue](config.eodCacheSize).unsafeRunSync())
  val client        = ForexClient.getClient[IO](config, Some(spiedNowishCache), Some(spiedEodCache))

  val spiedFx                = Forex[IO](config, client)
  val spiedFxWith5NowishSecs = Forex[IO](fxConfigWith5NowishSecs, client)

  implicit val timer = IO.timer(ExecutionContext.global)

  /**
   * nowish cache with 5-sec memory
   */
  "A lookup of CAD->GBP within memory time limit" should {
    "return the value stored in the nowish cache and be overwritten after configured time by a new request" in {
      val action = for {
        // call nowish, update the cache with key("CAD","GBP") and corresponding value
        _ <- spiedFxWith5NowishSecs.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish
        // get the value from the first HTPP request
        valueFromFirstHttpRequest <- spiedNowishCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP))
        // call nowish within 5 secs will get the value from the cache which is the same as valueFromFirstHttpRequest
        _ <- spiedFxWith5NowishSecs.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish

        valueFromCache <- spiedNowishCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP))
        test1 = valueFromCache must be equalTo valueFromFirstHttpRequest

        _ <- IO.sleep(6.seconds)
        // nowish will get the value over HTTP request, which will replace the previous value in the cache
        _ <- spiedFxWith5NowishSecs.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish

        // value will be different from previous value -
        // even if the monetary value is the same, the
        // timestamp will be different
        newValueFromCache <- spiedNowishCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP))
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
        .rate(CurrencyUnit.CAD)
        .to(CurrencyUnit.GBP)
        .eod(date)
        .productR(spiedEodCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP, date)))
        .map { valueFromCache =>
          there was two(spiedEodCache).get((CurrencyUnit.CAD, CurrencyUnit.GBP, date)) and (valueFromCache must beSome)
        }
        .unsafeRunSync()
    }
  }

}

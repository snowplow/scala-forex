/*
 * Copyright (c) 2013-2019 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.lrumap.CreateLruMap
import model._

/** Testing cache behaviours */
class SpiedCacheSpec extends Specification with Mockito {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key                     = sys.env.getOrElse("OER_KEY", "")
  val config                  = ForexConfig(key, DeveloperAccount)
  val fxConfigWith5NowishSecs = ForexConfig(key, DeveloperAccount, nowishSecs = 5)

  val spiedIoNowishCache = spy(
    CreateLruMap[IO, NowishCacheKey, NowishCacheValue].create(config.nowishCacheSize).unsafeRunSync()
  )
  val spiedIoEodCache = spy(
    CreateLruMap[IO, EodCacheKey, EodCacheValue].create(config.eodCacheSize).unsafeRunSync()
  )
  val ioClient = OerClient.getClient[IO](
    config,
    Some(spiedIoNowishCache),
    Some(spiedIoEodCache)
  )
  val spiedIoFx                = Forex[IO](config, ioClient)
  val spiedIoFxWith5NowishSecs = Forex[IO](fxConfigWith5NowishSecs, ioClient)

  implicit val timer = IO.timer(ExecutionContext.global)

  /**
    * nowish cache with 5-sec memory
    */
  "A lookup of CAD->GBP within memory time limit" should {
    "return the value stored in the nowish cache and be overwritten after configured time by a new request" in {
      val ioAction = for {
        // call nowish, update the cache with key("CAD","GBP") and corresponding value
        _ <- spiedIoFxWith5NowishSecs.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish
        // get the value from the first HTPP request
        valueFromFirstHttpRequest <- spiedIoNowishCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP))
        // call nowish within 5 secs will get the value from the cache which is the same as valueFromFirstHttpRequest
        _ <- spiedIoFxWith5NowishSecs.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish

        valueFromCache <- spiedIoNowishCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP))
        test1 = (valueFromCache must be).equalTo(valueFromFirstHttpRequest)

        _ <- IO.sleep(6.seconds)
        // nowish will get the value over HTTP request, which will replace the previous value in the cache
        _ <- spiedIoFxWith5NowishSecs.rate(CurrencyUnit.CAD).to(CurrencyUnit.GBP).nowish

        // value will be different from previous value -
        // even if the monetary value is the same, the
        // timestamp will be different
        newValueFromCache <- spiedIoNowishCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP))
        test2 = newValueFromCache mustNotEqual valueFromFirstHttpRequest
      } yield test1.and(test2)

      ioAction.unsafeRunSync()
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
      spiedIoFx
        .rate(CurrencyUnit.CAD)
        .to(CurrencyUnit.GBP)
        .eod(date)
        .productR(spiedIoEodCache.get((CurrencyUnit.CAD, CurrencyUnit.GBP, date)))
        .map { valueFromCache =>
          there
            .was(
              two(spiedIoEodCache).get(
                (CurrencyUnit.CAD, CurrencyUnit.GBP, date)
              )
            )
            .and(valueFromCache must beSome)
        }
        .unsafeRunSync()

    }
  }

}

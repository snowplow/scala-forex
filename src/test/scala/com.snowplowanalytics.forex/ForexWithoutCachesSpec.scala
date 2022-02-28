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

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import org.specs2.mutable.Specification

import model._

/** Testing that setting cache size to zero will disable the use of cache */
class ForexWithoutCachesSpec extends Specification {
  args(skipAll = sys.env.get("OER_KEY").isEmpty)

  val key = sys.env.getOrElse("OER_KEY", "")

  "Setting both cache sizes to zero" should {
    "disable the use of caches" in {
      val ioFxWithoutCache =
        CreateForex[IO].create(ForexConfig(key, DeveloperAccount, nowishCacheSize = 0, eodCacheSize = 0))
      ioFxWithoutCache.unsafeRunSync().client.eodCache.isEmpty
      ioFxWithoutCache.unsafeRunSync().client.nowishCache.isEmpty
    }
  }

}

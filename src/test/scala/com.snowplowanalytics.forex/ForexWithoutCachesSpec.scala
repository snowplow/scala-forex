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

// cats
import cats.effect.IO

// Specs2
import org.specs2.mutable.Specification
// TestHelpers
import TestHelpers._

/**
 * Testing that setting cache size to zero will disable the use of cache
 */
class ForexWithoutCachesSpec extends Specification {
  "Setting both cache sizes to zero" should {
    "disable the use of caches" in {
      val fxWithoutCache = Forex.getForex[IO](ForexConfig(nowishCacheSize = 0, eodCacheSize = 0), oerConfig)
      fxWithoutCache.client.eodCache.isEmpty
      fxWithoutCache.client.nowishCache.isEmpty
    }
  }

}

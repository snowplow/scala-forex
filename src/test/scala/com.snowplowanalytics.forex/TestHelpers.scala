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

// cats
import cats.effect.IO

// oerclient
import oerclient._

/**
 * All tests can have access to the same Forex object
 */
object TestHelpers {
  val key                     = sys.env("OER_KEY") // Warning: this will give nasty errors if env var not exported
  val config                  = ForexConfig() // ForexConfig object with default values
  val fxConfigWith5NowishSecs = ForexConfig(nowishSecs = 5) // ForexConfig object with 5 nowishSecs
  val oerConfig               = OerClientConfig(key, DeveloperAccount) // with default base currency USD
  val fx                      = Forex.getForex[IO](config, oerConfig) // Forex object with USD as base currency
  val forexConfig             = ForexConfig(nowishCacheSize = 0, eodCacheSize = 0)
  val fxWithoutCache          = Forex.getForex[IO](forexConfig, oerConfig) // Forex object with caches disabled
  val confWithBaseGBP         = OerClientConfig(key, EnterpriseAccount) // set base currency to GBP
  val fxWithBaseGBP           = Forex.getForex[IO](ForexConfig(baseCurrency = "GBP"), confWithBaseGBP) // Forex object with GBP as base currency
}

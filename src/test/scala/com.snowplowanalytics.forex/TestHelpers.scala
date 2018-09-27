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

// Joda
import org.joda.money.CurrencyUnit

// cats
import cats.effect.IO

object TestHelpers {
  val key                     = sys.env.getOrElse("OER_KEY", throw new RuntimeException("Provide OER_KEY variable"))
  val config                  = ForexConfig(key, DeveloperAccount) // ForexConfig object with default values
  val fxConfigWith5NowishSecs = ForexConfig(key, DeveloperAccount, nowishSecs = 5) // ForexConfig object with 5 nowishSecs
  val fx                      = Forex.getForex[IO](config) // Forex object with USD as base currency
  val forexConfig             = ForexConfig(key, DeveloperAccount, nowishCacheSize = 0, eodCacheSize = 0)
  val fxWithoutCache          = Forex.getForex[IO](forexConfig) // Forex object with caches disabled
  val fxWithBaseGBP           = Forex.getForex[IO](ForexConfig(key, EnterpriseAccount, baseCurrency = CurrencyUnit.GBP)) // Forex object with GBP as base currency
}

/*
 * Copyright (c) 2013-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics

import java.time.ZonedDateTime
import java.math.BigDecimal

import org.joda.money.CurrencyUnit

import com.snowplowanalytics.lrumap.LruMap
import forex.errors._

package object forex {

  /**
    * The key and value for each cache entry
    */
  type NowishCacheKey   = (CurrencyUnit, CurrencyUnit) // source currency , target currency
  type NowishCacheValue = (ZonedDateTime, BigDecimal)  // timestamp, exchange rate

  type EodCacheKey   = (CurrencyUnit, CurrencyUnit, ZonedDateTime) // source currency, target currency, timestamp
  type EodCacheValue = BigDecimal                                  // exchange rate

  // The API request either returns exchange rates in BigDecimal representation
  // or OerResponseError if the request failed
  type ApiRequestResult = Either[OerResponseError, BigDecimal]

  /**
    * The two LRU caches we use
    */
  type NowishCache[F[_]] = LruMap[F, NowishCacheKey, NowishCacheValue]
  type EodCache[F[_]]    = LruMap[F, EodCacheKey, EodCacheValue]
}

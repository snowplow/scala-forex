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
import java.math.BigDecimal
import java.time.ZonedDateTime

// cats
import cats.effect.Sync

// OpenExchangeRate client
import oerclient._
// LRUCache
import com.twitter.util.SynchronizedLruMap

/**
 * Companion object for ForexClient class
 * This class has one method for getting forex clients
 * but for now there is only one client since we are only using OER
 */
object ForexClient {

  /**
   * Getter for clients with specified caches(optional)
   */
  def getClient[F[_]: Sync](config: ForexConfig,
                            clientConfig: ForexClientConfig,
                            nowish: MaybeNowishCache = None,
                            eod: MaybeEodCache       = None): ForexClient[F] =
    clientConfig match {
      case oerClientConfig: OerClientConfig =>
        new OerClient[F](config, oerClientConfig, nowishCache = nowish, eodCache = eod)
      case _ => throw NoSuchClientException("This client is not supported by scala-forex currently")
    }
}

abstract class ForexClient[F[_]](config: ForexConfig,
                                 nowishCache: MaybeNowishCache = None,
                                 eodCache: MaybeEodCache       = None) {

  // Assemble our caches
  object caches {

    // LRU cache for nowish request, with (source currency, target currency) as the key
    // and (date time, exchange rate) as the value
    val nowish =
      if (nowishCache.isDefined) {
        nowishCache
      } else if (config.nowishCacheSize > 0) {
        Some(new SynchronizedLruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize))
      } else {
        None
      }

    // LRU cache for historical request, with (source currency, target currency, date time) as the key
    // and exchange rate as the value
    val eod =
      if (eodCache.isDefined) {
        eodCache
      } else if (config.eodCacheSize > 0) {
        Some(new SynchronizedLruMap[EodCacheKey, EodCacheValue](config.eodCacheSize))
      } else {
        None
      }
  }

  /**
   * Get the latest exchange rate from a given currency
   *
   * @param currency
   *            Desired currency
   * @return result returned from API
   */
  def getLiveCurrencyValue(currency: String): F[ApiRequestResult]

  /**
   * Get a historical exchange rate from a given currency and date
   *
   * @param currency
   *            Desired currency
   * @param date
   *            Date of desired rate
   * @return result returned from API
   */
  def getHistoricalCurrencyValue(currency: String, date: ZonedDateTime): F[ApiRequestResult]
}

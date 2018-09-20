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
import java.time.ZonedDateTime

// cats
import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.option._

// OpenExchangeRate client
import oerclient._

// LruMap
import com.snowplowanalytics.lrumap.LruMap

/**
 * Companion object for ForexClient class
 * This class has one method for getting forex clients
 * but for now there is only one client since we are only using OER
 */
object ForexClient {

  /**
   * Getter for clients, creating the caches as defined in the config
   */
  def getClient[F[_]: Sync](config: ForexConfig, clientConfig: ForexClientConfig): F[ForexClient[F]] =
    clientConfig match {
      case oerClientConfig: OerClientConfig =>
        val nowishCacheF =
          if (config.nowishCacheSize > 0)
            LruMap.create[F, NowishCacheKey, NowishCacheValue](config.nowishCacheSize).map(_.some)
          else Sync[F].pure(Option.empty[NowishCache[F]])

        val eodCacheF =
          if (config.eodCacheSize > 0)
            LruMap.create[F, EodCacheKey, EodCacheValue](config.eodCacheSize).map(_.some)
          else Sync[F].pure(Option.empty[EodCache[F]])

        (nowishCacheF, eodCacheF).mapN {
          case (nowish, eod) =>
            new OerClient[F](config, oerClientConfig, nowishCache = nowish, eodCache = eod)
        }
      case _ => throw NoSuchClientException("This client is not supported by scala-forex currently")
    }

  def getClient[F[_]: Sync](config: ForexConfig,
                            clientConfig: ForexClientConfig,
                            nowishCache: Option[NowishCache[F]],
                            eodCache: Option[EodCache[F]]): ForexClient[F] = clientConfig match {
    case oerConfig: OerClientConfig => new OerClient[F](config, oerConfig, nowishCache, eodCache)
    case _                          => throw new IllegalArgumentException("Unknown ForexClientConfig")
  }
}

abstract class ForexClient[F[_]](val config: ForexConfig,
                                 val nowishCache: Option[NowishCache[F]] = None,
                                 val eodCache: Option[EodCache[F]]       = None) {

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

/*
 * Copyright (c) 2013-2014 Snowplow Analytics Ltd. All rights reserved.
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
// joda
import org.joda.time._
// OpenExchangeRate client
import oerclient._
// LRUCache
import com.twitter.util.LruMap

/**
 * companion object for ForexClient trait
 */
object ForexClient {
  /**
   * Generate and get a spied OER Forex client
   * @return an Forex client
   */
  def getOerClient(config: ForexConfig, oerConfig: OerClientConfig,
                    nowish: NowishCache= None, eod: EodCache = None): ForexClient = {
    new OerClient(config, oerConfig, nowishCache = nowish, eodCache = eod)
  }
}

abstract class ForexClient(config: ForexConfig, nowishCacheFx: NowishCache = None,
                           eodCacheFx: EodCache  = None) {
  // LRU cache for nowish request, with tuple of source currency and target currency as the key
  // and tuple of time and exchange rate as the value 
  val nowishCache =
    if (nowishCacheFx.isDefined) {
      nowishCacheFx 
    } else if (config.nowishCacheSize > 0) {
        Some(new LruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize))
      } else {
        None
      }
    
  // LRU cache for historical request, with triple of source currency, target currency and time as the key 
  // and exchange rate as the value
  val eodCache = 
    if (eodCacheFx.isDefined) {
      eodCacheFx 
    } else if (config.eodCacheSize > 0) {
        Some(new LruMap[EodCacheKey, EodCacheValue](config.eodCacheSize))
      } else {
        None
      }
    
  /**
   * Get the latest exchange rate from a given currency
   * 
   * @param currency
   *            Desired currency
   * @return Latest value of exchange rate or OerResponseError object
   */
   def getLiveCurrencyValue(currency: String): Either[OerResponseError, BigDecimal]

  /**
   * Get a historical exchange rate from a given currency and date
   * 
   * @param currency
   *            Desired currency
   * @param date
   *            Date of desired rate
   * @return Value of exchange rate on desired date or OerResponseError object
   */
   def getHistoricalCurrencyValue(currency: String, date: DateTime): Either[OerResponseError, BigDecimal]
}

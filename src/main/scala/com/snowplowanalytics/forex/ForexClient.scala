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
import org.joda.money.CurrencyUnit
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
                    nowish: NowishCacheType = None, eod: EodCacheType = None): ForexClient = {
    new OerClient(config, oerConfig, spiedNowish = nowish, spiedEod = eod)
  }
}

abstract class ForexClient(config: ForexConfig, spiedNowishCache: NowishCacheType = None,
                           spiedEodCache: EodCacheType  = None) {
  // LRU cache for nowish request, with tuple of source currency and target currency as the key
  // and tuple of time and exchange rate as the value 
  val nowishCache =
    if (spiedNowishCache.isDefined) {
      spiedNowishCache 
    } else if (config.nowishCacheSize > 0) {
        Some(new LruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize))
      } else {
        None
      }
    
  // LRU cache for historical request, with triple of source currency, target currency and time as the key 
  // and exchange rate as the value
  val eodCache = 
    if (spiedEodCache.isDefined) {
      spiedEodCache 
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
   * @return Latest value of exchange rate or OerErr object
   */
   def getLiveCurrencyValue(currency: CurrencyUnit): Either[OerErr, BigDecimal]

  /**
   * Get a historical exchange rate from a given currency and date
   * 
   * @param currency
   *            Desired currency
   * @param date
   *            Date of desired rate
   * @return Value of exchange rate on desired date or OerErr object
   */
   def getHistoricalCurrencyValue(currency: CurrencyUnit, date: DateTime): Either[OerErr, BigDecimal]
}

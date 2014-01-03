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
// java
import java.math.BigDecimal
import java.util.Calendar
import java.util.Map
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
   * Generate and get a new OER Forex client
   * @return an Forex client
   */
  def getOerClient(config: ForexConfig): ForexClient = {
    new OerClient(config)
  }
} 


abstract class ForexClient(config: ForexConfig) {
  // LRU cache for nowish request, with tuple of source currency and target currency as the key
  // and tuple of time and exchange rate as the value 
  val nowishCacheOption = if (config.nowishCacheSize > 0) 
                          Some(new LruMap[NowishCacheKey, NowishCacheValue](config.nowishCacheSize))
                    else None
  // LRU cache for historical request, with triple of source currency, target currency and time as the key 
  // and exchange rate as the value
  val eodCacheOption = if (config.eodCacheSize > 0)
                            Some(new LruMap[EodCacheKey, EodCacheValue](config.eodCacheSize))
                        else None
  /**
   * Get the latest exchange rate from a given currency
   * 
   * @param currency
   *            Desired currency
   * @return Latest value of exchange rate
   */
   def getCurrencyValue(currency: CurrencyUnit):BigDecimal

  /**
   * Get a historical exchange rate from a given currency and date
   * 
   * @param currency
   *            Desired currency
   * @param date
   *            Date of desired rate
   * @return Value of exchange rate on desired date or error message if the date is invalid
   */
   def getHistoricalCurrencyValue(currency: CurrencyUnit, date: DateTime):Either[String, BigDecimal]
}

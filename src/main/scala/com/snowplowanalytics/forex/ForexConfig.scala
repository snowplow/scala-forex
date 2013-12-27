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
// joda
import org.joda.money.CurrencyUnit

/**
* user defined type for getNearestDay flag
*/
sealed trait EodRounding
object EodRoundDown extends EodRounding
object EodRoundUp extends EodRounding

/**
 * Configure class for Forex object
 *
 * @pvalue appId - key for the api
 * @pvalue configurableBase - flag for showing if the base currency is configurable 
 * @pvalue nowishCacheSize -  cache for nowish look up
 * @pvalue nowishSecs - time range for nowish look up
 * @pvalue historicalCacheSize -  cache for historical lookup
 * @pvalue getNearestDay - flag for deciding whether to get the exchange rate on closer day or previous day
 * @pvalue baseCurrency  - base currency is set to be USD by default if configurableBase flag is false, otherwise it is user-defined 
 */

case class ForexConfig(
  appId: String,
  configurableBase: Boolean,
  nowishCacheSize: Int         = 13530, // nowishCache = (165 * 164 / 2) = 13530.
  										// There are 165 currencies in total, the combinations of a currency pair
  										// has 165 * (165 - 1) possibilities. (X,Y) is the same as (Y,X) hence 165 * 164 / 2
  nowishSecs: Int              = 300,   // i.e. 5 mins by default 
  historicalCacheSize: Int     = 405900,  // 165 * 164 / 2 * 30 = 405900, assuming the cache stores data within a month
  getNearestDay: EodRounding   = EodRoundDown,
  baseCurrency: CurrencyUnit   = CurrencyUnit.USD  
) 

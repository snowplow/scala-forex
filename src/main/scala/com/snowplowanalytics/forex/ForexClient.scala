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

/**
 * companion object for ForexClient trait
 */
object ForexClient {
	/**
	 * Generate and get a new Forex client
	 * @return an Forex client
	 */
	def getClient(fx: Forex, apiKey: String): ForexClient = {
		new OerClient(fx, apiKey)
	}
} 


trait ForexClient {
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
	 * @return Value of exchange rate in desired date
	 */
	 def getHistoricalCurrencyValue(currency: CurrencyUnit, date: DateTime):BigDecimal
}

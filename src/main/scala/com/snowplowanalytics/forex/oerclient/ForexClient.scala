/* Copyright 2012 Demétrio de Castro Menezes Neto
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package com.snowplowanalytics.forex.oerclient

import java.math.BigDecimal
import java.util.Calendar
import java.util.Map
import org.joda.money.CurrencyUnit

/**
 * companion object 
 */
object ForexClient {

	/**
	 * Generate and get a new Open Exchange Rates client
	 * @return a Open Exchange Rates client
	 */
	def getClient(apiKey: String):OpenExchangeRates = {
		new OerClient(apiKey)
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
	 def getHistoricalCurrencyValue(currency: CurrencyUnit, date: Calendar):BigDecimal
}

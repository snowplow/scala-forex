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
package org.openexchangerates.oerjava;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;

import org.openexchangerates.oerjava.exceptions.UnavailableExchangeRateException;

/**
 * Open Exchange Rates(http://openexchangerates.org) client
 * @author Demétrio Menezes Neto
 */
public abstract class OpenExchangeRates {

	/**
	 * Generate and get a new Open Exchange Rates client
	 * @return a Open Exchange Rates client
	 */
	public static OpenExchangeRates getClient(String apiKey) {
		return new OpenExchangeRatesJsonClient(apiKey);
	}

	/**
	 * Get the latest exchange rates from
	 * http://openexchangerates.org/latest.json
	 * 
	 * @return Last updated exchange rates
	 */
	public abstract Map<Currency, BigDecimal> getLatest();

	/**
	 * Get a historical exchange rate from a given date
	 * 
	 * @param date
	 *            Date of desired rates
	 * @return Exchange rates of desired date.
	 * @throws UnavailableExchangeRateException
	 *             when a exchange rate is unavailable
	 */
	public abstract Map<Currency, BigDecimal> getHistorical(Calendar date)
			throws UnavailableExchangeRateException;

	/**
	 * Get the latest exchange rate from a given currency
	 * 
	 * @param currency
	 *            Desired currency
	 * @return Latest value of exchange rate
	 */
	public abstract BigDecimal getCurrencyValue(Currency currency);

	/**
	 * Get a historical exchange rate from a given currency and date
	 * 
	 * @param currency
	 *            Currency of desired rate
	 * @param date
	 *            Date of desired rate
	 * @return Value of exchange rate in desired date
	 * @throws when
	 *             a exchange rate is unavailable
	 */
	public abstract BigDecimal getHistoricalCurrencyValue(Currency currency,
			Calendar date) throws UnavailableExchangeRateException;
}

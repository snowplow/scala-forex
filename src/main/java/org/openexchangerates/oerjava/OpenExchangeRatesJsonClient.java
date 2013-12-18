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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openexchangerates.oerjava.exceptions.UnavailableExchangeRateException;

/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * 
 * @author Demétrio Menezes Neto
 */
class OpenExchangeRatesJsonClient extends OpenExchangeRates {
	private final static String OER_URL = "http://openexchangerates.org/api/";
	private String LATEST;
	private String HISTORICAL;

	private final static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Constructor for a new OpenExchangeRatesJsonClient
	 *
	 * @param apiKey The API key to Open Exchange Rates
	 */
	public OpenExchangeRatesJsonClient(String apiKey) {
		LATEST = "latest.json?app_id=" + apiKey;
		HISTORICAL = "historical/%04d-%02d-%02d.json?app_id=" + apiKey;
	}

	/**
	 * Downloads the exchanges rates from given json path
	 * 
	 * @param downloadPath
	 *            Path to json file
	 * @return Map containing all rates of json file
	 * @throws UnavailableExchangeRateException
	 */
	private static Map<Currency, BigDecimal> downloadExchangeRates(
			String downloadPath) throws UnavailableExchangeRateException {
		Map<Currency, BigDecimal> exchangeRates = new HashMap<Currency, BigDecimal>();
		try {
			URL url = new URL(OER_URL + downloadPath);
			URLConnection conn = url.openConnection();
			JsonNode node = mapper.readTree(conn.getInputStream());
			for (Currency currency : Currency.values()) {
				JsonNode currencyNode = node.findValue(currency.name());
				if (currencyNode != null) {
					BigDecimal value = currencyNode.getDecimalValue();
					exchangeRates.put(currency, value);
				} else {
					// Old currency e.g: ZWD(1980-2008)
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			throw new UnavailableExchangeRateException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return exchangeRates;
	}

	/**
	 * Get the latest exchange rates
	 * 
	 * @return Last updated exchange rates
	 */
	public Map<Currency, BigDecimal> getLatest() {
		Map<Currency, BigDecimal> exchangeRates = null;
		try {
			exchangeRates = downloadExchangeRates(LATEST);
		} catch (UnavailableExchangeRateException e) {
			// Never will happen;
		}
		return exchangeRates;
	}

	public Map<Currency, BigDecimal> getHistorical(Calendar date)
			throws UnavailableExchangeRateException {

		int day = date.get(Calendar.DAY_OF_MONTH);
		int month = date.get(Calendar.MONTH) + 1;
		int year = date.get(Calendar.YEAR);

		String historical = String.format(HISTORICAL, year, month, day);
		return downloadExchangeRates(historical);

	}

	public BigDecimal getCurrencyValue(Currency currency) {
		return getLatest().get(currency);
	}

	public BigDecimal getHistoricalCurrencyValue(Currency currency,
			Calendar date) throws UnavailableExchangeRateException {

		return getHistorical(date).get(currency);

	}
}

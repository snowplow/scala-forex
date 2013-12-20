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
package com.snowplowanalytics.forex.oerclient;

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

/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * 
 * @author Demétrio Menezes Neto
 */
class OerJsonClient extends OpenExchangeRates {
	private final static String OER_URL = "http://openexchangerates.org/api/";
	
	/**
	 * The constant that will holds the URI for
	 * a latest-exchange rate lookup from OER
	 */
	private String LATEST;
	
	/**
	 *
	 */
	private String HISTORICAL;

	private final static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Constructor for a new OpenExchangeRatesJsonClient
	 *
	 * @param apiKey The API key to Open Exchange Rates
	 */
	public OerJsonClient(String apiKey) {
		LATEST = "latest.json?app_id=" + apiKey;
		HISTORICAL = "historical/%04d-%02d-%02d.json?app_id=" + apiKey;
	}

	public BigDecimal getCurrencyValue(String currency)  throws UnavailableExchangeRateException{
		return getExchangeRates(LATEST, currency);
	}

	public BigDecimal getHistoricalCurrencyValue(String currency,
			Calendar date) throws UnavailableExchangeRateException {
		int day = date.get(Calendar.DAY_OF_MONTH);
		int month = date.get(Calendar.MONTH) + 1;
		int year = date.get(Calendar.YEAR);
		String historical = String.format(HISTORICAL, year, month, day);
		return getExchangeRates(historical, currency);
	}

	private static BigDecimal getExchangeRates(
			String downloadPath, String currency) throws UnavailableExchangeRateException {
		
		BigDecimal exchangeRate = new BigDecimal(0);
		
		try {
		
			URL url = new URL(OER_URL + downloadPath);
			URLConnection conn = url.openConnection();
			JsonNode node = mapper.readTree(conn.getInputStream());

			exchangeRate = node.findValue(currency).getDecimalValue();

	    } catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			throw new UnavailableExchangeRateException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return exchangeRate;
	}
}


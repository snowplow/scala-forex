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


package com.snowplowanalytics.forex.oerclient

// Java
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigDecimal
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.util.Calendar

// Json
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

// Joda money
import org.joda.money.CurrencyUnit

import com.snowplowanalytics.forex.ForexClient

/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * @param apiKey The API key to Open Exchange Rates
 */
class OerClient(apiKey: String) extends ForexClient {
	private val oerUrl = "http://openexchangerates.org/api/"
	/**
	 * The constant that will hold the URI for
	 * a latest-exchange rate lookup from OER
	 */
	private val lastest = "latest.json?app_id=" + apiKey
	/**
	 * The constant will hold the URI for a 
	 * historical-exchange rate lookup from OER
	 */
	private var historical = "historical/%04d-%02d-%02d.json?app_id=" + apiKey
	private val mapper = new ObjectMapper()

	/**
	 *  
	 */
	def getCurrencyValue(currency: CurrencyUnit):BigDecimal = {
		getExchangeRates(lastest, currency)
	}

	def getHistoricalCurrencyValue(currency: CurrencyUnit, date: Calendar ): BigDecimal = {
		val day   	   = date.get(Calendar.DAY_OF_MONTH)
		val month 	   = date.get(Calendar.MONTH) + 1
		val year  	   = date.get(Calendar.YEAR)
		val historicalLink = historical.format(year, month, day)
		getExchangeRates(historicalLink, currency)
	}

	private def getExchangeRates(downloadPath: String, currency: CurrencyUnit): BigDecimal = {
		try {
			val url = new URL(oerUrl + downloadPath)
			val conn = url.openConnection()
			val node = mapper.readTree(conn.getInputStream())
			node.findValue(currency.toString).getDecimalValue()
	    } catch {
	    	case (e: FileNotFoundException) => throw new UnavailableExchangeRateException("exchange rate unavailable")
	    	case (e: MalformedURLException) => throw new MalformedURLException("invalid URL")
			case (e: IOException) =>           throw new IOException(e.getMessage)
		}
	}
}


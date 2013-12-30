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
// Joda 
import org.joda.money._
import org.joda.time._
// LRUCache
import com.twitter.util.LruMap
// forexClient
import com.snowplowanalytics.forex.ForexClient
// forexConfig
import com.snowplowanalytics.forex.ForexConfig
// Forex
import com.snowplowanalytics.forex.Forex


/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * @param apiKey The API key to Open Exchange Rates
 */

class OerClient(config: ForexConfig) extends ForexClient(config) {
	private val oerUrl = "http://openexchangerates.org/api/"
	// sets the base currency in the url 
	private val base   = if (config.configurableBase) "&base=" + config.baseCurrency 
						           else ""
	/**
	 * The constant that will hold the URI for
	 * a latest-exchange rate lookup from OER
	 */
	private val lastest = "latest.json?app_id=" + config.appId + base
	/**
	 * The constant will hold the URI for a 
	 * historical-exchange rate lookup from OER
	 */
	private var historical = "historical/%04d-%02d-%02d.json?app_id=" + config.appId + base
	private val mapper = new ObjectMapper()
	def getCurrencyValue(currency: CurrencyUnit): BigDecimal= {
	  val key = new Tuple2(config.baseCurrency, currency) 
	  nowishCache.get(key) match {
      case Some(value) =>
                val (date, rate) = value
                rate 
     	case None        => 
                val node = getJsonNodeFromAPI(lastest, currency)
                val currencyNameIterator = node.getFieldNames
                while (currencyNameIterator.hasNext) {  
                  val currencyName = currencyNameIterator.next
                  try {
                 	  val keyPair   = new Tuple2(config.baseCurrency, CurrencyUnit.getInstance(currencyName))
                	  val valuePair = new Tuple2(DateTime.now, node.findValue(currencyName).getDecimalValue)                                                                       
                    nowishCache.put(keyPair, valuePair)
                	} catch {
                	  case (e: IllegalCurrencyException) => {}
                	}
                }
                node.findValue(currency.toString).getDecimalValue
		}
	}

	def getHistoricalCurrencyValue(currency: CurrencyUnit, date: DateTime): BigDecimal = {
    val dateCal    = date.toGregorianCalendar
	  val day   	   = dateCal.get(Calendar.DAY_OF_MONTH)
	  val month 	   = dateCal.get(Calendar.MONTH) + 1
		val year  	   = dateCal.get(Calendar.YEAR)
		val historicalLink = historical.format(year, month, day)
		val key = new Tuple3(config.baseCurrency, currency, date) 
		eodCache.get(key) match {
	    case Some(rate) =>  
                rate
	    case None       =>
                val node = getJsonNodeFromAPI(historicalLink, currency)
                val currencyNameIterator = node.getFieldNames 
                while (currencyNameIterator.hasNext) {  
                  val currencyName = currencyNameIterator.next
                  try {
                    val keySet  = new Tuple3(config.baseCurrency, CurrencyUnit.getInstance(currencyName), date)
                 	  eodCache.put(keySet, node.findValue(currencyName).getDecimalValue)
                  } catch{
                    case (e: IllegalCurrencyException) => {}
                  }		                          			                          	
                }
                node.findValue(currency.toString).getDecimalValue
    }  
	}

	// helper method which returns the node which contains a list of currency and rate pair
	private def getJsonNodeFromAPI(downloadPath: String, currency: CurrencyUnit): JsonNode = {
	  val url  = new URL(oerUrl + downloadPath)
	  val conn = url.openConnection
	  val root = mapper.readTree(conn.getInputStream).getElements
    var resNode       = root.next
    while (root.hasNext) {
      resNode = root.next
    }
    resNode
	}

}


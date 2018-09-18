/*
 * Copyright (c) 2013-2017 Snowplow Analytics Ltd. All rights reserved.
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
package oerclient

// Java
import java.net.URL
import java.net.HttpURLConnection
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
// Json
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * @param config - a configurator for Forex object
 * @param oerConfig - a configurator for OER Client object
 * @param nowishCache - user defined nowishCache
 * @param eodCache - user defined eodCache
 */
class OerClient(
  config: ForexConfig,
  oerConfig: OerClientConfig,
  nowishCache: MaybeNowishCache = None,
  eodCache: MaybeEodCache       = None
) extends ForexClient(config, nowishCache, eodCache) {

  /** Base URL to OER API */
  private val oerUrl = "http://openexchangerates.org/api/"

  /** Sets the base currency in the url
   * according to the API, only Unlimited and Enterprise accounts
   * are allowed to set the base currency in the HTTP URL
   */
  private val base = oerConfig.accountLevel match {
    case UnlimitedAccount  => "&base=" + config.baseCurrency
    case EnterpriseAccount => "&base=" + config.baseCurrency
    case DeveloperAccount  => ""
  }

  /**
   * The constant that will hold the URL for
   * a live exchange rate lookup from OER
   */
  private val latest = "latest.json?app_id=" + oerConfig.appId + base

  /** Mapper for reading JSON objects */
  private val mapper = new ObjectMapper()

  /** The earliest date OER service is availble */
  private val oerDataFrom = ZonedDateTime.of(LocalDateTime.of(1999, 1, 1, 0, 0), ZoneId.systemDefault)

  /**
   * Gets live currency value for the desired currency,
   * silently drop the currency types which Joda money does not support.
   * If cache exists, update nowishCache when an API request has been done,
   * else just return the forex rate
   * @param currency - The desired currency we want to look up from the API
   * @return result returned from API
   */
  def getLiveCurrencyValue(currency: String): ApiRequestResult =
    getJsonNodeFromApi(latest) match {
      case Left(oerResponseError) => Left(oerResponseError)
      case Right(node) => {
        nowishCache match {
          case Some(cache) => {
            val currencyNameIterator = node.getFieldNames
            oerConfig.accountLevel match {
              // If the user is using Developer account,
              // then base currency returned from the API is USD.
              // To store user-defined base currency into the cache,
              // we need to convert the forex rate between target currency and USD
              // to target currency and user-defined base currency
              case DeveloperAccount => {
                val usdOverBase = node.findValue(config.baseCurrency).getDecimalValue
                while (currencyNameIterator.hasNext) {
                  val currencyName = currencyNameIterator.next
                  val keyPair      = (config.baseCurrency, currencyName)
                  val usdOverCurr  = node.findValue(currencyName).getDecimalValue
                  // flag indicating if the base currency has been set to USD
                  val fromCurrIsBaseCurr = (config.baseCurrency == "USD")
                  val baseOverCurr       = Forex.getForexRate(fromCurrIsBaseCurr, usdOverBase, usdOverCurr)
                  val valPair            = (ZonedDateTime.now, baseOverCurr)
                  cache.put(keyPair, valPair)
                }
              }
              // For Enterprise and Unlimited users, OER allows them to configure the base currencies.
              // So the exchange rate returned from the API is between target currency and the base currency they defined.
              case _ => {
                while (currencyNameIterator.hasNext) {
                  val currencyName = currencyNameIterator.next
                  val keyPair      = (config.baseCurrency, currencyName)
                  val valPair      = (ZonedDateTime.now, node.findValue(currencyName).getDecimalValue)
                  cache.put(keyPair, valPair)
                }
              }
            }
          }
          case None => // do nothing
        }
        val currencyNode = node.findValue(currency)
        if (currencyNode == null) {
          Left(OerResponseError("Currency not found in the API, invalid currency ", IllegalCurrency))
        } else {
          Right(currencyNode.getDecimalValue)
        }
      }
    }

  /**
   * Builds the historical link for the URI according to the date
   * @param date - The historical date for the currency look up,
   * which should be the same as date argument in the getHistoricalCurrencyValue method below
   * @return the link in string format
   */
  private def buildHistoricalLink(date: ZonedDateTime): String = {
    val historical = "historical/%04d-%02d-%02d.json?app_id=" + oerConfig.appId + base
    historical.format(date.getYear, date.getMonthValue, date.getDayOfMonth)
  }

  /**
   * Gets historical forex rate for the given currency and date
   * return error message if the date is invalid
   * silently drop the currency types which Joda money does not support
   * if cache exists, update the eodCache when an API request has been done,
   * else just return the look up result
   * @param currency - The desired currency we want to look up from the API
   * @param date - The specific date we want to look up on
   * @return result returned from API
   */
  def getHistoricalCurrencyValue(currency: String, date: ZonedDateTime): ApiRequestResult =
    /**
     * Return OerResponseError if the date given is not supported by OER
     */
    if (date.isBefore(oerDataFrom) || date.isAfter(ZonedDateTime.now)) {
      Left(OerResponseError("Exchange rate unavailable on the date [%s] ".format(date), ResourcesNotAvailable))
    } else {
      val historicalLink = buildHistoricalLink(date)
      getJsonNodeFromApi(historicalLink) match {
        case Left(oerResponseError) => Left(oerResponseError)
        case Right(node) => {
          eodCache match {
            case Some(cache) => {
              val currencyNameIterator = node.getFieldNames
              oerConfig.accountLevel match {
                // If the user is using Developer account,
                // then base currency returned from the API is USD.
                // To store user-defined base currency into the cache,
                // we need to convert the forex rate between target currency and USD
                // to target currency and user-defined base currency
                case DeveloperAccount => {
                  val usdOverBase = node.findValue(config.baseCurrency).getDecimalValue
                  while (currencyNameIterator.hasNext) {
                    val currencyName       = currencyNameIterator.next
                    val keyPair            = (config.baseCurrency, currencyName, date)
                    val usdOverCurr        = node.findValue(currencyName).getDecimalValue
                    val fromCurrIsBaseCurr = (config.baseCurrency == "USD")
                    cache.put(keyPair, Forex.getForexRate(fromCurrIsBaseCurr, usdOverBase, usdOverCurr))
                  }
                }
                // For Enterprise and Unlimited users, OER allows them to configure the base currencies.
                // So the exchange rate returned from the API is between target currency and the base currency they defined.
                case _ => {
                  while (currencyNameIterator.hasNext) {
                    val currencyName = currencyNameIterator.next
                    val keyPair      = (config.baseCurrency, currencyName, date)
                    cache.put(keyPair, node.findValue(currencyName).getDecimalValue)
                  }
                }
              }
            }
            case None => // do nothing
          }
          val currencyNode = node.findValue(currency)
          if (currencyNode == null) {
            Left(OerResponseError("Currency not found in the API, invalid currency ", IllegalCurrency))
          } else {
            Right(currencyNode.getDecimalValue)
          }
        }
      }
    }

  /**
   * Helper method which returns the node containing
   * a list of currency and rate pair.
   * @param downloadPath - The URI link for the API request
   * @return JSON node which contains currency information obtained from API
   * or OerResponseError object which carries the error message returned by the API
   */
  private def getJsonNodeFromApi(downloadPath: String): Either[OerResponseError, JsonNode] = {
    val url  = new URL(oerUrl + downloadPath)
    val conn = url.openConnection
    conn match {
      case (httpUrlConn: HttpURLConnection) => {
        if (httpUrlConn.getResponseCode >= 400) {
          val errorStream = httpUrlConn.getErrorStream
          val root        = mapper.readTree(errorStream).getElements
          var resNode     = root.next
          while (root.hasNext) {
            resNode = root.next
          }
          Left(OerResponseError(resNode.getTextValue, OtherErrors))
        } else {
          val inputStream = httpUrlConn.getInputStream
          val root        = mapper.readTree(inputStream).getElements
          var resNode     = root.next
          while (root.hasNext) {
            resNode = root.next
          }
          Right(resNode)
        }
      }
      case _ => throw new ClassCastException
    }
  }
}

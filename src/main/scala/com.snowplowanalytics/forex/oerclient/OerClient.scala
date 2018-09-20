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
import java.math.{BigDecimal => JBigDecimal}

// cats
import cats.effect.Sync
import cats.implicits._
import cats.data.EitherT

// circe
import io.circe.parser.parse
import io.circe.generic.JsonCodec

@JsonCodec final case class OerResponse(base: String, rates: Map[String, BigDecimal])

/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * @param config - a configurator for Forex object
 * @param oerConfig - a configurator for OER Client object
 * @param nowishCache - user defined nowishCache
 * @param eodCache - user defined eodCache
 */
class OerClient[F[_]: Sync](
  config: ForexConfig,
  oerConfig: OerClientConfig,
  nowishCache: Option[NowishCache[F]] = None,
  eodCache: Option[EodCache[F]]       = None
) extends ForexClient[F](config, nowishCache, eodCache) {

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
  def getLiveCurrencyValue(currency: String): F[ApiRequestResult] = {
    val action = for {
      response     <- EitherT(getResponseFromApi(latest))
      liveCurrency <- EitherT(extractLiveCurrency(response, currency))
    } yield liveCurrency

    action.value
  }

  private def extractLiveCurrency(response: OerResponse, currency: String): F[ApiRequestResult] = {
    val cacheAction = nowishCache.traverse { cache =>
      oerConfig.accountLevel match {
        // If the user is using Developer account,
        // then base currency returned from the API is USD.
        // To store user-defined base currency into the cache,
        // we need to convert the forex rate between target currency and USD
        // to target currency and user-defined base currency
        case DeveloperAccount =>
          val usdOverBase = response.rates(config.baseCurrency)
          response.rates.toList.traverse_ {
            case (currencyName, usdOverCurr) =>
              val keyPair = (config.baseCurrency, currencyName)
              // flag indicating if the base currency has been set to USD
              val fromCurrIsBaseCurr = config.baseCurrency == "USD"
              val baseOverCurr =
                Forex.getForexRate(fromCurrIsBaseCurr, usdOverBase.bigDecimal, usdOverCurr.bigDecimal)
              val valPair = (ZonedDateTime.now, baseOverCurr)
              cache.put(keyPair, valPair)
          }
        // For Enterprise and Unlimited users, OER allows them to configure the base currencies.
        // So the exchange rate returned from the API is between target currency and the base currency they defined.
        case _ =>
          response.rates.toList.traverse_ {
            case (currencyName, currencyValue) =>
              val keyPair = (config.baseCurrency, currencyName)
              val valPair = (ZonedDateTime.now, currencyValue.bigDecimal)
              cache.put(keyPair, valPair)
          }
      }
    }
    cacheAction.map { _ =>
      response.rates
        .get(currency)
        .map(_.bigDecimal)
        .toRight(OerResponseError("Currency not found in the API, invalid currency ", IllegalCurrency))
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
  def getHistoricalCurrencyValue(currency: String, date: ZonedDateTime): F[ApiRequestResult] =
    if (date.isBefore(oerDataFrom) || date.isAfter(ZonedDateTime.now)) {
      OerResponseError("Exchange rate unavailable on the date [%s] ".format(date), ResourcesNotAvailable)
        .asLeft[JBigDecimal]
        .pure[F]
    } else {
      val historicalLink = buildHistoricalLink(date)
      val action = for {
        response <- EitherT(getResponseFromApi(historicalLink))
        currency <- EitherT(extractHistoricalCurrency(response, currency, date))
      } yield currency

      action.value
    }

  private def extractHistoricalCurrency(response: OerResponse,
                                        currency: String,
                                        date: ZonedDateTime): F[ApiRequestResult] = {
    val cacheAction = eodCache.traverse { cache =>
      oerConfig.accountLevel match {
        // If the user is using Developer account,
        // then base currency returned from the API is USD.
        // To store user-defined base currency into the cache,
        // we need to convert the forex rate between target currency and USD
        // to target currency and user-defined base currency
        case DeveloperAccount =>
          val usdOverBase = response.rates(config.baseCurrency)
          Sync[F].delay(response.rates.foreach {
            case (currencyName, usdOverCurr) =>
              val keyPair            = (config.baseCurrency, currencyName, date)
              val fromCurrIsBaseCurr = config.baseCurrency == "USD"
              cache.put(keyPair, Forex.getForexRate(fromCurrIsBaseCurr, usdOverBase.bigDecimal, usdOverCurr.bigDecimal))
          })
        // For Enterprise and Unlimited users, OER allows them to configure the base currencies.
        // So the exchange rate returned from the API is between target currency and the base currency they defined.
        case _ =>
          Sync[F].delay(response.rates.foreach {
            case (currencyName, currencyValue) =>
              val keyPair = (config.baseCurrency, currencyName, date)
              cache.put(keyPair, currencyValue.bigDecimal)
          })
      }
    }

    cacheAction.map(
      _ =>
        response.rates
          .get(currency)
          .map(_.bigDecimal)
          .toRight(OerResponseError(s"Currency not found in the API, invalid currency $currency", IllegalCurrency)))
  }

  /**
   * Helper method which returns the node containing
   * a list of currency and rate pair.
   * @param downloadPath - The URI link for the API request
   * @return JSON node which contains currency information obtained from API
   * or OerResponseError object which carries the error message returned by the API
   */
  private def getResponseFromApi(downloadPath: String): F[Either[OerResponseError, OerResponse]] = Sync[F].delay {
    val url  = new URL(oerUrl + downloadPath)
    val conn = url.openConnection
    conn match {
      case httpUrlConn: HttpURLConnection =>
        if (httpUrlConn.getResponseCode >= 400) {
          val errorString = scala.io.Source.fromInputStream(httpUrlConn.getErrorStream).mkString
          parse(errorString)
            .flatMap(_.hcursor.downField("message").as[String])
            .leftMap(e => OerResponseError(e.getMessage, OtherErrors))
            .flatMap(message => Left(OerResponseError(message, OtherErrors)))
        } else {
          parse(scala.io.Source.fromInputStream(httpUrlConn.getInputStream).mkString)
            .flatMap(_.as[OerResponse])
            .leftMap(e => OerResponseError(e.getMessage, OtherErrors))
        }
      case _ => throw new ClassCastException
    }
  }
}

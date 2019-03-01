/*
 * Copyright (c) 2013-2018 Snowplow Analytics Ltd. All rights reserved.
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

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.math.{BigDecimal => JBigDecimal}

import cats.effect.Sync
import cats.implicits._
import cats.data.EitherT
import org.joda.money.CurrencyUnit

import com.snowplowanalytics.lrumap.CreateLruMap
import errors._
import model._
import responses._

/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * @param config - a configurator for Forex object
 * @param nowishCache - user defined nowishCache
 * @param eodCache - user defined eodCache
 */
case class OerClient[F[_]: Sync](
  config: ForexConfig,
  nowishCache: Option[NowishCache[F]] = None,
  eodCache: Option[EodCache[F]]       = None,
  transport: Transport[F]
) {

  private val endpoint = "openexchangerates.org/api/"

  /** Sets the base currency in the url
   * according to the API, only Unlimited and Enterprise accounts
   * are allowed to set the base currency in the HTTP URL
   */
  private val base = config.accountLevel match {
    case UnlimitedAccount  => "&base=" + config.baseCurrency
    case EnterpriseAccount => "&base=" + config.baseCurrency
    case DeveloperAccount  => ""
  }

  /**
   * The constant that will hold the URL for
   * a live exchange rate lookup from OER
   */
  private val latest = "latest.json?app_id=" + config.appId + base

  /** The earliest date OER service is availble */
  private val oerDataFrom =
    ZonedDateTime.of(LocalDateTime.of(1999, 1, 1, 0, 0), ZoneId.systemDefault)

  /**
   * Gets live currency value for the desired currency,
   * silently drop the currency types which Joda money does not support.
   * If cache exists, update nowishCache when an API request has been done,
   * else just return the forex rate
   * @param currency - The desired currency we want to look up from the API
   * @return result returned from API
   */
  def getLiveCurrencyValue(currency: CurrencyUnit): F[ApiRequestResult] = {
    val action = for {
      response     <- EitherT(transport.receive(endpoint, latest))
      liveCurrency <- EitherT(extractLiveCurrency(response, currency))
    } yield liveCurrency
    action.value
  }

  private def extractLiveCurrency(
    response: OerResponse,
    currency: CurrencyUnit
  ): F[ApiRequestResult] = {
    val cacheAction = nowishCache.traverse { cache =>
      config.accountLevel match {
        // If the user is using Developer account,
        // then base currency returned from the API is USD.
        // To store user-defined base currency into the cache,
        // we need to convert the forex rate between target currency and USD
        // to target currency and user-defined base currency
        case DeveloperAccount =>
          val usdOverBase = response.rates(config.baseCurrency)
          response.rates.toList.traverse_ {
            case (currentCurrency, usdOverCurr) =>
              val keyPair = (config.baseCurrency, currentCurrency)
              // flag indicating if the base currency has been set to USD
              val fromCurrIsBaseCurr = config.baseCurrency == CurrencyUnit.USD
              val baseOverCurr =
                Forex.getForexRate(fromCurrIsBaseCurr, usdOverBase.bigDecimal, usdOverCurr.bigDecimal)
              val valPair = (ZonedDateTime.now, baseOverCurr)
              cache.put(keyPair, valPair)
          }
        // For Enterprise and Unlimited users, OER allows them to configure the base currencies.
        // So the exchange rate returned from the API is between target currency and the base
        // currency they defined.
        case _ =>
          response.rates.toList.traverse_ {
            case (currentCurrency, currencyValue) =>
              val keyPair = (config.baseCurrency, currentCurrency)
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
  private def buildHistoricalLink(date: ZonedDateTime): String =
    f"historical/${date.getYear}%04d-${date.getMonthValue}%02d-${date.getDayOfMonth}%02d.json?app_id=${config.appId}" + base

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
  def getHistoricalCurrencyValue(
    currency: CurrencyUnit,
    date: ZonedDateTime
  ): F[ApiRequestResult] =
    if (date.isBefore(oerDataFrom) || date.isAfter(ZonedDateTime.now)) {
      OerResponseError(s"Exchange rate unavailable on the date [$date]", ResourcesNotAvailable)
        .asLeft[JBigDecimal]
        .pure[F]
    } else {
      val historicalLink = buildHistoricalLink(date)
      val action = for {
        response <- EitherT(transport.receive(endpoint, historicalLink))
        currency <- EitherT(extractHistoricalCurrency(response, currency, date))
      } yield currency

      action.value
    }

  private def extractHistoricalCurrency(
    response: OerResponse,
    currency: CurrencyUnit,
    date: ZonedDateTime
  ): F[ApiRequestResult] = {
    val cacheAction = eodCache.traverse { cache =>
      config.accountLevel match {
        // If the user is using Developer account,
        // then base currency returned from the API is USD.
        // To store user-defined base currency into the cache,
        // we need to convert the forex rate between target currency and USD
        // to target currency and user-defined base currency
        case DeveloperAccount =>
          val usdOverBase = response.rates(config.baseCurrency)
          Sync[F].delay(response.rates.foreach {
            case (currentCurrency, usdOverCurr) =>
              val keyPair            = (config.baseCurrency, currentCurrency, date)
              val fromCurrIsBaseCurr = config.baseCurrency == CurrencyUnit.USD
              cache.put(
                keyPair,
                Forex.getForexRate(
                  fromCurrIsBaseCurr,
                  usdOverBase.bigDecimal,
                  usdOverCurr.bigDecimal
                )
              )
          })
        // For Enterprise and Unlimited users, OER allows them to configure the base currencies.
        // So the exchange rate returned from the API is between target currency and the base
        // currency they defined.
        case _ =>
          Sync[F].delay(response.rates.foreach {
            case (currentCurrency, currencyValue) =>
              val keyPair = (config.baseCurrency, currentCurrency, date)
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
}

/**
 * Companion object for ForexClient class
 * This class has one method for getting forex clients
 * but for now there is only one client since we are only using OER
 */
object OerClient {

  /** Creates a client with a cache and sensible default ForexConfig */
  def getClient[F[_]: Sync](appId: String, accountLevel: AccountType): F[OerClient[F]] =
    getClient[F](ForexConfig(appId = appId, accountLevel = accountLevel))

  /** Getter for clients, creating the caches as defined in the config */
  def getClient[F[_]: Sync](
    config: ForexConfig
  )(
    implicit CLM1: CreateLruMap[F, NowishCacheKey, NowishCacheValue],
    CLM2: CreateLruMap[F, EodCacheKey, EodCacheValue]
  ): F[OerClient[F]] = {
    val nowishCacheF =
      if (config.nowishCacheSize > 0) {
        CLM1.create(config.nowishCacheSize).map(_.some)
      } else {
        Sync[F].pure(Option.empty[NowishCache[F]])
      }

    val eodCacheF =
      if (config.eodCacheSize > 0) {
        CLM2.create(config.eodCacheSize).map(_.some)
      } else {
        Sync[F].pure(Option.empty[EodCache[F]])
      }

    (nowishCacheF, eodCacheF).mapN {
      case (nowish, eod) =>
        new OerClient[F](config, nowishCache = nowish, eodCache = eod, Transport.httpTransport[F])
    }
  }

  def getClient[F[_]: Sync](
    config: ForexConfig,
    nowishCache: Option[NowishCache[F]],
    eodCache: Option[EodCache[F]],
    transport: Transport[F]
  ): OerClient[F] = new OerClient[F](config, nowishCache, eodCache, transport)
}

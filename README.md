# Scala Forex

[![Build Status][travis-image]][travis]
[![Maven Central][maven-image]][maven-link]
[![License][license-image]][license]
[![Join the chat at https://gitter.im/snowplow/scala-forex][gitter-image]][gitter-link]
[![codecov](https://codecov.io/gh/snowplow/scala-forex/branch/master/graph/badge.svg)](https://codecov.io/gh/snowplow/scala-forex)

## 1. Introduction

Scala Forex is a high-performance Scala library for performing exchange rate lookups and currency conversions, using [Joda-Money][joda-money].

It includes configurable LRU (Least Recently Used) caches to minimize calls to the API; this makes the library usable in high-volume environments such as Hadoop and Storm.

Currently Scala Forex uses the [Open Exchange Rates API][oer-signup] to perform currency lookups.

## 2. Setup

### 2.1 OER Sign Up

First [sign up][oer-signup] to Open Exchange Rates to get your App ID for API access.

There are three types of accounts supported by OER API, Unlimited, Enterprise and Developer levels. See the [sign up][oer-signup] page for specific account descriptions. For Scala Forex, we recommend an Enterprise or Unlimited account, unless all of your conversions are to or from USD (see section 4.5 OER accounts for an explanation). For 10-minute rate updates, you will need an Unlimited account (other accounts are hourly).

### 2.2 Installation

The latest version of Scala Forex is 0.7.0, which is built against 2.12.x.

If you're using SBT, add the following lines to your build file:

```scala
libraryDependencies ++= Seq(
  "com.snowplowanalytics" %% "scala-forex" % "0.7.0"
)
```

Note the double percent (`%%`) between the group and artifactId. That'll ensure you get the right package for your Scala version.

## 3. Usage

The Scala Forex library supports two types of usage:

1. Exchange rate lookups
2. Currency conversions

Both usage types support live, near-live or historical (end-of-day) exchange rates.

For all code samples below we are assuming the following imports:

```scala
import org.joda.money.CurrencyUnit
import com.snowplowanalytics.forex._
```

### 3.1 Configuration

Scala Forex is configured via `ForexConfig` case class. Except `appId` and `accountLevel` fields, it provides
some sensible defaults.

```scala
case class ForexConfig(
  appId: String,
  accountLevel: AccountType,
  nowishCacheSize: Int       = 13530,
  nowishSecs: Int            = 300,
  eodCacheSize: Int          = 405900,
  getNearestDay: EodRounding = EodRoundDown,
  baseCurrency: CurrencyUnit = CurrencyUnit.USD
)
```

To go through each in turn:

1. `appId` is the API key you get from OER.

2. `accountLevel` is the type of OER account you have. Possible values are `UnlimitedAccount`, `EnterpriseAccount`, and `DeveloperAccount`.

1. `nowishCacheSize` is the size configuration for near-live (nowish) lookup cache, it can be disabled by setting its value to 0. The key to nowish cache is a currency pair so the size of the cache equals to the number of pairs of currencies available.

2. `nowishSecs` is the time configuration for near-live lookup. A call to this cache will use the exchange rates stored in nowish cache if its time stamp is less than or equal to `nowishSecs` old.

3. `eodCacheSize` is the size configuration for end-of-day (eod) lookup cache, it can be disabled by setting its value to 0. The key to eod cache is a tuple of currency pair and time stamp, so the size of eod cache equals to the number of currency pairs times the days which the cache will remember the data for.

4. `getNearestDay` is the rounding configuration for latest eod (at) lookup. The lookup will be performed on the next day if the rounding mode is set to EodRoundUp, and on the previous day if EodRoundDown.

5. `baseCurrency` can be configured to different currencies by the users.

For an explanation for the default values please see section **5.4 Explanation of defaults** below.

### 3.2 Rate lookup

Unless specified otherwise, assume `forex` value is initialized as:

```scala
val config: ForexConfig = ForexConfig("YOUR_API_KEY", DeveloperAccount)
def fForex[F[_]: Sync]: F[Forex] = CreateForex[F].create(config)
```

`CreateForex[F].create` returns `F[Forex]` instead of `Forex`, because creation of the underlying
caches is a side effect. You can `flatMap` over the result (or use a for-comprehension, as seen
below). All examples below return `F[Either[OerResponseError, Money]]`, which means they are not
executed.

If you don't care about side effects, we also provide instance of `Forex` for `cats.Eval` and
`cats.Id`:

```scala
val evalForex: Eval[Forex] = CreateForex[Eval].create(config)
val idForex: Forex = CreateForex[Id].create(config)
```

For distributed applications, such as Spark or Beam apps, where lazy values might be an issue, you may want to use the `Id` instance.

#### 3.2.1 Live rate

Look up a live rate _(no caching available)_:

```scala
// USD => JPY
val usd2jpyF: F[Either[OerResponseError, Money]] = for {
  fx     <- fForex
  result <- fx.rate.to(CurrencyUnit.JPY).now
} yield result

// using Eval
val usd2jpyE: Eval[Either[OerResponseError, Money]] = for {
  fx     <- evalForex
  result <- fx.rate.to(CurrencyUnit.JPY).now
} yield result

// using Id
val usd2jpyI: Either[OerResponseError, Money] =
  idForex.rate.to(CurrencyUnit.JPY).now
```

#### 3.2.2 Near-live rate

Look up a near-live rate _(caching available)_:

```scala
// JPY => GBP
val jpy2gbp = for {
  fx     <- fForex
  result <- fx.rate(CurrencyUnit.JPY).to(CurrencyUnit.GBP).nowish
} yield result
```

#### 3.2.3 Near-live rate without cache

Look up a near-live rate (_uses cache selectively_):

```scala
// JPY => GBP
val jpy2gbp = for {
  fx     <- CreateForex[IO].create(ForexConfig("YOU_API_KEY", DeveloperAccount, nowishCacheSize = 0))
  result <- fx.rate(CurrencyUnit.JPY).to(CurrencyUnit.GBP).nowish
} yield result
```

#### 3.2.4 Latest-prior EOD rate

Look up the latest EOD (end-of-date) rate prior to your event _(caching available)_:

```scala
import java.time.{ZonedDateTime, ZoneId}

// USD => JPY at the end of 13/03/2011
val tradeDate = ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))
val usd2jpy = for {
  fx     <- fForex
  result <- fx.rate.to(CurrencyUnit.JPY).at(tradeDate)
} yield result
```

#### 3.2.5 Latest-post EOD rate

Look up the latest EOD (end-of-date) rate post to your event _(caching available)_:

```scala
// USD => JPY at the end of 13/03/2011
val tradeDate = ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))
val usd2jpy = for {
  fx     <- Forex.getClient[IO](ForexConfig("YOU_API_KEY", DeveloperAccount, getNearestDay = EodRoundUp))
  result <- fx.rate.to(CurrencyUnit.JPY).at(tradeDate)
} yield result
```

#### 3.2.6 Specific EOD rate

Look up the EOD rate for a specific date _(caching available)_:

```scala
// GBP => JPY at the end of 13/03/2011
val tradeDate = ZonedDateTime.of(2011, 3, 13, 0, 0, 0, 0, ZoneId.of("America/New_York"))
val gbp2jpy = for {
  fx     <- Forex.getClient[IO](ForexConfig("YOU_API_KEY", EnterpriseAccount, baseCurrency= CurrencyUnit.GBP))
  result <- fx.rate.to(CurrencyUnit.JPY).eod(eodDate)
} yield result
```

#### 3.2.7 Specific EOD rate without cache

Look up the EOD rate for a specific date _(no caching)_:

```scala
// GBP => JPY at the end of 13/03/2011
val tradeDate = ZonedDateTime.of(2011, 3, 13, 0, 0, 0, 0, ZoneId.of("America/New_York"))
val gbp2jpy = for {
  fx     <- Forex.getClient[IO](ForexConfig("YOU_API_KEY", EnterPriseAccount,
              baseCurrency = CurrencyUnit.GBP, eodCacheSize = 0))
  result <- fx.rate.to(CurrencyUnit.JPY).eod(eodDate)
} yield result
```

### 3.3 Currency conversion

#### 3.3.1 Live rate

Conversion using the live exchange rate _(no caching available)_:

```scala
// 9.99 USD => EUR
val priceInEuros = for {
  fx     <- forex
  result <- fx.convert(9.99).to(CurrencyUnit.EUR).now
} yield result
```

#### 3.3.2 Near-live rate

Conversion using a near-live exchange rate with 500 seconds `nowishSecs` _(caching available)_:

```scala
// 9.99 GBP => EUR
val priceInEuros = for {
  fx     <- CreateForex[IO].create(ForexConfig("YOU_API_KEY", DeveloperAccount, nowishSecs = 500))
  result <- fx.convert(9.99, CurrencyUnit.GBP).to(CurrencyUnit.EUR).nowish
} yield result
```

#### 3.3.3 Near-live rate without cache

Note that this will be a live rate conversion if cache is not available.
Conversion using a live exchange rate with 500 seconds `nowishSecs`,
this conversion will be done via HTTP request:

```scala
// 9.99 GBP => EUR
val priceInEuros = for {
  fx     <- CreateForex[IO].create(ForexConfig("YOUR_API_KEY", DeveloperAccount, nowishSecs = 500, nowishCacheSize = 0))
  result <- fx.convert(9.99, CurrencyUnit.GBP).to(CurrencyUnit.EUR).nowish
} yield result
```

#### 3.3.4 Latest-prior EOD rate

Conversion using the latest EOD (end-of-date) rate prior to your event _(caching available)_:

```scala
// 10000 GBP => JPY at the end of 12/03/2011
val tradeDate = ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))
val tradeInYen = for {
  fx     <- forex
  result <- fx.convert(10000, CurrencyUnit.GBP).to(CurrencyUnit.JPY).at(tradeDate)
} yield result
```

#### 3.3.5 Latest-post EOD rate

Lookup the latest EOD (end-of-date) rate following your event _(caching available)_:

```scala
// 10000 GBP => JPY at the end of 13/03/2011
val tradeDate = ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))
val usd2jpy = for {
  fx     <- CreateForex[IO].create(ForexConfig("Your API key / app id", DeveloperAccount, getNearestDay = EodRoundUp))
  result <- fx.convert(10000, CurrencyUnit.GBP).to(CurrencyUnit.JPY).at(tradeDate)
} yield result
```

#### 3.3.6 Specific EOD rate

Conversion using the EOD rate for a specific date _(caching available)_:

```scala
// 10000 GBP => JPY at the end of 13/03/2011
val eodDate = ZonedDateTime.of(2011, 3, 13, 11, 39, 27, 567, ZoneId.of("America/New_York"))
val tradeInYen = for {
  fx     <- Forex.getClient[IO](ForexConfig("YOU_API_KEY", DeveloperAccount, baseCurrency = CurrencyUnit.GBP))
  result <- fx.convert(10000).to(CurrencyUnit.JPY).eod(eodDate)
} yield result
```

#### 3.3.7 Specific EOD rate without cache

Conversion using the EOD rate for a specific date, _(no caching)_:

```scala
// 10000 GBP => JPY at the end of 13/03/2011
val eodDate = ZonedDateTime.of(2011, 3, 13, 0, 0, 0, 0, ZoneId.of("America/New_York"))
val tradeInYen = for {
  fx     <- Forex.getClient[IO](ForexConfig("YOU_API_KEY", DeveloperAccount,
              baseCurrency = CurrencyUnit.GBP, eodCacheSize = 0))
  result <- fx.convert(10000).to(CurrencyUnit.JPY).eod(eodDate)
} yield result
```

### 3.4 Usage notes

#### 3.4.1 LRU cache

The `eodCacheSize` and `nowishCacheSize` values determine the maximum number of values to keep in the LRU cache,
which the Client will check prior to making an API lookup. To disable either LRU cache, set its size to zero,
i.e. `eodCacheSize = 0`.

#### 3.4.2 From currency selection

A default _"from currency"_ can be specified for all operations, using the `baseCurrency` argument to the `ForexConfig` object.
If not specified, `baseCurrency` is set to USD by default.

## 4. Development

### 4.1 Running tests

You **must** export your `OER_KEY` or else the tests will be skipped. To run the test suite locally:

```
$ export OER_KEY=<<key>>
$ git clone https://github.com/snowplow/scala-forex.git
$ cd scala-forex
$ sbt test
```

## 5. Implementation details

### 5.1 End-of-day definition

The end of today is 00:00 on the next day.

### 5.2 Exchange rate lookup

When `.now` is specified, the **live** exchange rate available from Open Exchange Rates is used.

When `.nowish` is specified, a **cached** version of the **live** exchange rate is used, if the timestamp of that exchange rate is less than or equal to `nowishSecs` (see above) ago. Otherwise a new lookup is performed.

When `.at(...)` is specified, the **latest end-of-day rate prior** to the datetime is used by default. Or users can configure that Scala Forex "round up" to the end of the occurring day.

When `.eod(...)` is specified, the end-of-day rate for the **specified day** is used. Any hour/minute/second/etc portion of the datetime is ignored.

### 5.3 LRU cache

We recommend trying different LRU cache sizes to see what works best for you.

Please note that the LRU cache implementation is **not** thread-safe. Switch it off if you are working with threads.

### 5.4 Explanation of defaults

#### 5.4.1 `nowishCache` = (165 * 164 / 2) = 13530

There are 165 currencies provided by the OER API, hence 165 * 164 pairs of currency combinations.
The key in nowish cache is a tuple of source currency and target currency, and the nowish cache was implemented in a way such that a lookup from CurrencyA to CurrencyB or from CurrencyB to CurrencyA will use the same exchange rate, so we don't need to store both in the caches. Hence there are (165 * 164 / 2) pairs of currencies for nowish cache.

#### 5.4.2 `eodCache` = (165 * 164 / 2) * 30 = 405900

Assume the eod cache stores the rates to each pair of currencies for 1 month(i.e. 30 days).
There are 165 * 164 / 2 pairs of currencies, hence (165 * 164 / 2) * 30 entries.

#### 5.4.3 `nowishSecs` = 300

Assume nowish cache stores data for 5 mins.

#### 5.4.4 `getNearestDay` = EodRoundDown

By convention, we are always interested in the exchange rates prior to the query date, hence EodRoundDown.

#### 5.4.5 `baseCurrency` = USD

We selected USD for the base currency because this is the OER default as well.

### 5.5 OER accounts

With Open Exchange Rates' Unlimited and Enterprise accounts, Scala Forex can specify the base currency to use when looking up exchange rates; Developer-level accounts will always retrieve rates against USD, so a rate lookup from e.g. GBY to EUR will require two conversions (GBY -> USD -> EUR). For this reason, we recommend Unlimited and Enterprise-level accounts for slightly more accurate non-USD-related lookups.

## 6. Documentation

The Scaladoc pages for this library can be found [here][scaladoc-pages].

## 7. Copyright and license

Scala Forex is copyright 2013-2021 Snowplow Analytics Ltd.

Licensed under the [Apache License, Version 2.0][license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[oer-signup]: https://openexchangerates.org/signup?r=snowplow

[joda-money]: http://www.joda.org/joda-money/

[travis]: https://travis-ci.org/snowplow/scala-forex
[travis-image]: https://travis-ci.org/snowplow/scala-forex.png?branch=master

[maven-image]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/scala-forex_2.12/badge.svg
[maven-link]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/scala-forex_2.12

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[gitter-image]: https://badges.gitter.im/snowplow/scala-forex.svg
[gitter-link]: https://gitter.im/snowplow/scala-forex?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge

[scaladoc-pages]: http://snowplow.github.io/scala-forex/

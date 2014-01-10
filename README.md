# Scala Forex [![Build Status](https://travis-ci.org/snowplow/scala-forex.png)](https://travis-ci.org/snowplow/scala-forex)

## 1. Introduction

Scala Forex is a high-performance Scala library for performing exchange rate lookups and currency conversions, using [Joda-Money] [joda-money] and [Joda-Time] [joda-time].

It includes configurable LRU (Least Recently Used) caches to minimize calls to the API; this makes the library usable in high-volume environments such as Hadoop and Storm.

Currently Scala Forex uses the [Open Exchange Rates API] [oer-api] to perform currency lookups.

## 2. Setup

### 2.1 OER Sign Up

First [sign up] [oer-signup] to Open Exchange Rates to get your App ID for API access.

There are three types of accounts supported by OER API, Unlimited, Enterprise and Developer levels. See the [sign up] [oer-signup] page for specific account descriptions. For Scala Forex, we recommend an Enterprise or Unlimited account, unless all of your conversions are to or from USD (see section 4.5 OER accounts for an explanation).

### 2.2 Configuration

Scala Forex is configured via two case classes:

1. `ForexConfig` contains general configuration
2. `OerClientConfig` contains Open Exchange Rates-specific configuration

#### 2.2.1 ForexConfig

Case class with defaults:

```scala
case class ForexConfig(
  nowishCacheSize: Int         = 13530, 
  nowishSecs: Int              = 300,  
  eodCacheSize: Int            = 405900,  
  getNearestDay: EodRounding   = EodRoundDown,
  baseCurrency: String   = "USD"  
) 
``` 

To go through each in turn:

1. `nowishCacheSize` is the size configuration for near-live(nowish) lookup cache, it can be disabled by setting its value to 0. The key to nowish cache is a currency pair so the size of the cache equals to the number of pairs of currencies available.

2. `nowishSecs` is the time configuration for near-live lookup. Nowish call will use the exchange rates stored in nowish cache if its time stamp is less than or equal to `nowishSecs` old.

3. `eodCacheSize` is the size configuration for end-of-day(eod) lookup cache, it can be disabled by setting its value to 0. The key to eod cache is a tuple of currency pair and time stamp, so the size of eod cache equals to the number of currency pairs times the days which the cache will remember the data for.

4. `getNearestDay` is the rounding configuration for latest eod(at) lookup. The lookup will be performed on the next day if the rounding mode is set to EodRoundUp, and on the previous day if EodRoundDown.

5. `baseCurrency` can be configured to different currencies by the users. 


For an explanation for the default values please see section **4.4 Explanation of defaults** below.

#### 2.2.2 OerClientConfig

Case class with defaults:

```scala
case class OerClientConfig(
  appId: String,            
  accountLevel: AccountType 
) extends ForexClientConfig
``` 

To go through each in turn:

1. `appId` is the unique key for the user's account
2. `accountLevel` is the account type provided by the user which should obviously be consistent with the app ID.
There are three types of account levels, users should provide the exact account type name to configure the OER Client:
  1. UnlimitedAccount
  2. EnterpriseAccount
  3. DeveloperAccount

### 2.3 REPL setup

To try out Scala Forex in the Scala REPL:

```
$ git clone https://github.com/snowplow/scala-forex.git
$ cd scala-forex
$ sbt console
...
scala> val appId = "<<key>>"
scala> import com.snowplowanalytics.forex.{Forex, ForexConfig}
scala> import com.snowplowanalytics.forex.oerclient.{OerClientConfig, <<Your AccountType>>} 
  e.g. import com.snowplowanalytics.forex.oerclient.{OerClientConfig, DeveloperAccount}  
```

### 2.4 Running tests

To run the Scala Forex test suite locally:

```
$ export OER_KEY=<<key>>
$ git clone https://github.com/snowplow/scala-forex.git
$ cd scala-forex
$ sbt test
```

## 3. Usage

The Scala Forex supports two types of usage:

1. Exchange rate lookups
2. Currency conversions

Both usage types support live, near-live or historical (end-of-day) exchange rates.

### 3.1 Rate lookup

For the required imports, please see section **2.3 REPL setup** above.

#### 3.1.1 Live rate

Lookup a live rate _(no cacheing available)_:

```scala
// USD => JPY
val fx = Forex(ForexConfig(), OerClientConfig(appId, DeveloperAccount))
val usd2jpy = fx.rate.to("JPY").now   // => Right(JPY 105)           
```

#### 3.1.2 Near-live rate

Lookup a near-live rate _(cacheing available)_:

```scala
// JPY => GBP
val fx = Forex(ForexConfig(), OerClientConfig(appId, DeveloperAccount))
val jpy2gbp = fx.rate("JPY").to("GBP").nowish   // => Right(GBP 0.01)
```

#### 3.1.3 Near-live rate without cache

Lookup a near-live rate (_uses cache selectively_):

```scala
// JPY => GBP
val fx = Forex(ForexConfig(nowishCacheSize = 0), OerClientConfig(appId, DeveloperAccount))
val jpy2gbp = fx.rate("JPY").to("GBP").nowish   // => Right(GBP 0.01)
```

#### 3.1.4 Latest-prior EOD rate

Lookup the latest EOD (end-of-date) rate prior to your event _(cacheing available)_:

```scala
import org.joda.time.{DateTime, DateTimeZone}

// USD => JPY at the end of 12/03/2011 
val fx = Forex(ForexConfig(), OerClientConfig(appId, DeveloperAccount)) // round down to previous day by default
val tradeDate = new DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val usd2yen = fx.rate.to("JPY").at(tradeDate)   // => Right(JPY 82)
```

#### 3.1.5 Latest-post EOD rate 

Lookup the latest EOD (end-of-date) rate post to your event _(cacheing available)_:

```scala
import org.joda.time.{DateTime, DateTimeZone}
import com.snowplowanalytics.forex.EodRoundUp

// USD => JPY at the end of 13/03/2011 
val fx = Forex(ForexConfig(getNearestDay = EodRoundUp), OerClientConfig(appId, DeveloperAccount)) 
val tradeDate = new DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val usd2yen = fx.rate.to("JPY").at(tradeDate)   // => Right(JPY 82)
```

#### 3.1.6 Specific EOD rate

Lookup the EOD rate for a specific date _(cacheing available)_:

```scala
import org.joda.time.DateTime

// GBP => JPY at the end of 13/03/2011
val fx = Forex(ForexConfig(baseCurrency="GBP"), OerClientConfig(appId, EnterpriseAccount)) // Your app ID should be an Enterprise account
val eodDate = new DateTime(2011, 3, 13, 0, 0)
val gbp2jpy = fx.rate.to("JPY").eod(eodDate)   // => Right(JPY 131)
```

#### 3.1.7 Specific EOD rate without cache

Lookup the EOD rate for a specific date _(no cacheing)_: 

```scala
import org.joda.time.DateTime

// GBP => JPY at the end of 13/03/2011
val fx = Forex(ForexConfig(eodCacheSize = 0, baseCurrency="GBP"), OerClientConfig(appId, EnterpriseAccount)) // Your app ID should be an Enterprise account
val eodDate = new DateTime(2011, 3, 13, 0, 0)
val gbp2jpy = fx.rate.to("JPY").eod(eodDate)   // => Right(JPY 131)
```

### 3.2 Currency conversion

For the required imports, please see section **2.3 REPL setup** above.

#### 3.2.1 Live rate

Conversion using the live exchange rate _(no cacheing available)_:

```scala
// 9.99 USD => EUR
val fx = Forex(ForexConfig(), OerClientConfig(appId, DeveloperAccount))
val priceInEuros = fx.convert(9.99).to("EUR").now 
```

#### 3.2.2 Near-live rate

Conversion using a near-live exchange rate with 500 seconds nowishSecs _(cacheing available)_:

```scala
// 9.99 GBP => EUR 
val fx = Forex(ForexConfig(nowishSecs = 500), OerClientConfig(appId, DeveloperAccount))
val priceInEuros = fx.convert(9.99, "GBP").to("EUR").nowish
```

#### 3.2.3 Near-live rate without cache

Note that this will be a live rate conversion if cache is not available.
Conversion using a live exchange rate with 500 seconds nowishSecs,
this conversion will be done via HTTP request: 

```scala

// 9.99 GBP => EUR
val fx = Forex(ForexConfig(nowishSecs = 500, nowishCacheSize = 0), OerClientConfig(appId, DeveloperAccount))
val priceInEuros = fx.convert(9.99, "GBP").to("EUR").nowish
```

#### 3.2.4 Latest-prior EOD rate

Conversion using the latest EOD (end-of-date) rate prior to your event _(cacheing available)_:

```scala
import org.joda.time.{DateTime, DateTimeZone}

// 10000 GBP => JPY at the end of 12/03/2011 
val fx = Forex(ForexConfig(), OerClientConfig(appId, DeveloperAccount))
val tradeDate = new DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val tradeInYen = fx.convert(10000, "GBP").to("JPY").at(tradeDate)                   
```

#### 3.2.5 Latest-post EOD rate 

Lookup the latest EOD (end-of-date) rate following your event _(cacheing available)_:

```scala
import org.joda.time.{DateTime, DateTimeZone}
import com.snowplowanalytics.forex.EodRoundUp

// 10000 GBP => JPY at the end of 13/03/2011
val fx = Forex(ForexConfig(getNearestDay = EodRoundUp), OerClientConfig(appId, DeveloperAccount)) 
val tradeDate = new DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val usd2yen = fx.convert(10000, "GBP").to("JPY").at(tradeDate) 
```

#### 3.2.6 Specific EOD rate

Conversion using the EOD rate for a specific date _(cacheing available)_:

```scala
import org.joda.time.DateTime

// 10000 GBP => JPY at the end of 13/03/2011
val fx = Forex(ForexConfig(baseCurrency="GBP"), OerClientConfig(appId, DeveloperAccount))
val eodDate = new DateTime(2011, 3, 13, 0, 0)
val tradeInYen = fx.convert(10000).to("JPY").eod(eodDate)
```

#### 3.2.7 Specific EOD rate without cache

Conversion using the EOD rate for a specific date, _(no cacheing)_: 

```scala
import org.joda.time.DateTime

// 10000 GBP => JPY at the end of 13/03/2011
val fx = Forex(ForexConfig(eodCacheSize = 0, baseCurrency="GBP"), OerClientConfig(appId, DeveloperAccount))
val eodDate = new DateTime(2011, 3, 13, 0, 0)
val tradeInYen = fx.convert(10000).to("JPY").eod(eodDate)
```

### 3.3 Usage notes

#### 3.3.1 LRU cache

The `lruCache` value determines the maximum number of values to keep in the LRU cache, which the Client will check prior to making an API lookup. To disable the LRU cache, set its size to zero, i.e. `lruCache = 0`.

#### 3.3.2 From currency selection

A default _"from currency"_ can be specified for all operations, using the `baseCurrency` argument to the `ForexConfig` object.
If not specified, `baseCurrency` is set to USD by default.


## 4. Implementation details

### 4.1 End-of-day definition

The end of today is 00:00 on the next day.

### 4.2 Exchange rate lookup

When `.now` is specified, the **live** exchange rate available from Open Exchange Rates is used.

When `.nowish` is specified, a **cached** version of the **live** exchange rate is used, if the timestamp of that exchange rate is less than or equal to `nowishSecs` (see above) ago. Otherwise a new lookup is performed.

When `.at(...)` is specified, the **latest end-of-day rate prior** to the datetime is used by default. Users can configure so that the rate on that specific date is used.
  
When `.eod(...)` is specified, the end-of-day rate for the **specified day** is used. Any hour/minute/second/etc portion of the datetime is ignored.

### 4.3 LRU cache

We recommend trying different LRU cache sizes to see what works best for you.

Please note that the LRU cache implementation is **not** thread-safe ([see this note] [twitter-lru-cache]). Switch it off if you are working with threads.

### 4.4 Explanation of defaults

#### 4.4.1 `nowishCache` = (165 * 164 / 2) = 13530 

There are 165 currencies provided by the OER API, hence 165 * 164 pairs of currency combinations.
The key in nowish cache is a tuple of source currency and target currency, and the nowish cache was implemented in a way such that a lookup from CurrencyA to CurrencyB or from CurrencyB to CurrencyA will use the same exchange rate, so we don't need to store both in the caches. Hence there are (165 * 164 / 2) pairs of currencies for nowish cache.

#### 4.4.2 `eodCache` = (165 * 164 / 2) * 30 = 405900

Assume the eod cache stores the rates to each pair of currencies for 1 month(i.e. 30 days).
There are 165 * 164 / 2 pairs of currencies, hence (165 * 164 / 2) * 30 entries.

#### 4.4.3 `nowishSecs` = 300

Assume nowish cache stores data for 5 mins.

#### 4.4.4 `getNearestDay` = EodRoundDown

By convention, we are always interested in the exchange rates prior to the query date, hence EodRoundDown.

#### 4.4.5 `baseCurrency` = USD

We selected USD for the base currency because this is the OER default as well.

#### 4.5 OER accounts

With Open Exchange Rates' Unlimited and Enterprise accounts, Scala Forex can specify the base currency to use when looking up exchange rates; Developer-level accounts will always retrieve rates against USD, so a rate lookup from e.g. GBY to EUR will require two conversions (GBY -> USD -> EUR). For this reason, we recommend Unlimited and Enterprise-level accounts for slightly more accurate non-USD-related lookups.

## 5. Authors

* [Jiawen Zhou] [jz4112]: all development and testing; documentation
* [Alex Dean] [alexanderdean]: API design; some documentation

## 6. Copyright and license

Scala Forex is copyright 2013-2014 Snowplow Analytics Ltd.

Licensed under the [Apache License, Version 2.0] [license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[oer-api]: https://openexchangerates.org/
[oer-scala]: https://github.com/snowplow/scala-forex/blob/master/src/main/scala/com/snowplowanalytics/forex/oerclient/OerClient.scala
[oer-signup]: https://openexchangerates.org/signup

[joda-money]: http://www.joda.org/joda-money/
[joda-time]: http://www.joda.org/joda-time/

[twitter-lru-cache]: http://twitter.github.com/commons/apidocs/com/twitter/common/util/caching/LRUCache.html

[jz4112]: https://github.com/jz4112
[alexanderdean]: https://github.com/alexanderdean

[license]: http://www.apache.org/licenses/LICENSE-2.0

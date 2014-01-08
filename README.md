# Scala Forex [![Build Status](https://travis-ci.org/snowplow/scala-forex.png)](https://travis-ci.org/snowplow/scala-forex)

**PRE-RELEASE.**

## Introduction

Forex Client is a high-performance Scala library for performing currency conversions.

It includes a configurable LRU (Least Recently Used) cache to minimize calls to the API; this makes the library usable in high-volume environments such as Hadoop and Storm.

Currently Forex Client uses the [Open Exchange Rates API] [oer-api] to perform currency lookups.

Forex Client for foreign exchange is built on top of [Joda-Money] [joda-money] and [Joda-Time] [joda-time].

## Installation

First [sign up] [oer-signup] to Open Exchange Rates to get your App ID for API access.

There are three types of accounts, note that Enterprise and Unlimited allow users to configure the base currency, but OER Client will automatically convert between currencies.

## Configure Forex Client 

ForexConfig contains only general configuration options:

'nowishCacheSize' is the size configuration for near-live(nowish) lookup cache, it can be disabled by setting its value to 0.

'nowishSecs' is the time configuration for near-live lookup. The exchange rate will be returned if its time stamp is less than or equal to 'nowishSecs' old.

'eodCacheSize' is the size configuration for end-of-day(eod) lookup cache, it can be disabled by setting its value to 0.

'getNearestDay' is the rounding configuration for latest prior eod(at) lookup. The lookup will be performed on the next day if the rounding mode is set to EodRoundUp, and on the previous day if EodRoundDown.

'baseCurrency' can only be set to other currencies other than USD if the user has Unlimited or Enterprise account, if it is set to other currencies the configurableBase value in OerClient has to be set to true accordingly.    

All configurations in Forex Client are set to recommended values, but users are free to set them to desired values. 

Explanation for the default values see Section 3 : Usage notes.
```scala
case class ForexConfig(
  nowishCacheSize: Int         = 13530, 
  nowishSecs: Int              = 300,  
  eodCacheSize: Int            = 405900,  
  getNearestDay: EodRounding   = EodRoundDown,
  baseCurrency: CurrencyUnit   = CurrencyUnit.USD  
) 
``` 

OerClientConfig has congurations specific to the OER API:

'appId' is the unique key for the user's account,

'configurableBase' is a boolean value indicating if the base currency can be configured. The baseCurrency value in ForexConfig can be changed if the boolean value is set to true. Note that only Enterprise and Unlimited users are allowed to set the value to true.

```scala
case class OerClientConfig(
  appId: String,            
  configurableBase: Boolean  
)  
```

## Usage

The OER Scala Client supports two types of usage:

1. Exchange rate lookups
2. Currency conversions

Both usage types support live, near-live or historical (end-of-day) exchange rates.

#### IMPORTANT:
run "export OER_KEY=>>insert your app id here<<" in terminal before you do any queries, this is for setting the environment variable which is your unique app id.
Use the following command to get the id. 
```scala
val appId = sys.env("OER_KEY") 
```


### 1. Rate lookup

#### Live rate

Lookup a live rate _(no cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(ForexConfig(), OerClientConfig(appId, false))
val usd2jpy = fx.rate().to("JPY").now // if argument for rate() is not specified, it will be set to base currency by default
```

### Near-live rate

Lookup a near-live rate _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(ForexConfig(), OerClientConfig(appId, false))
val jpy2gbp = fx.rate("JPY").to("GBP").nowish 
```

### Latest-prior EOD rate

Lookup the latest EOD (end-of-date) rate prior to your event _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(ForexConfig(eodCacheSize = 0, getNearestDay = EodRoundUp), OerClientConfig(appId, false)) // disable cache 
val tradeDate = DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val usd2yen = fx.rate().to("JPY").at(tradeDate) // always get the exchange rate on the next day
```

### Specific EOD rate

Lookup the EOD rate for a specific date _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(ForexConfig(baseCurrency="GBP"), OerClientConfig(appId, true))
val eodDate = DateTime(2011, 3, 13, 0, 0)
val gbp2jpy = fx.rate.to("JPY").eod(eodDate) 
```

### 2. Currency conversion

#### Live rate

Conversion using the live exchange rate _(no cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(ForexConfig(), OerClientConfig(appId, false))
val priceInEuros = fx.convert(9.99).to("EUR").now // => convert 9.99 units in base currency to EURO
```

#### Near-live rate

Conversion using a near-live exchange rate _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(ForexConfig(nowishSecs = 500), OerClientConfig(appId, false))
val priceInEuros = fx.convert(9.99, "GBP").to("EUR").nowish // => convert 9.99 pounds to EURO according to the rates within 															// the last 500 secs or the next 500 secs
```

#### Latest-prior EOD rate

Conversion using the latest EOD (end-of-date) rate prior to your event _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(ForexConfig(), OerClientConfig(appId, false))
val tradeDate = DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val tradeInYen = fx.convert(10000, "GBP").to("JPY").at(tradeDate) // => convert 10000 pounds to JPY according to the eod rate  																	 // on the day prior to the trade date
```

#### Specific EOD rate

Conversion using the EOD rate for a specific date _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(ForexConfig(baseCurrency="GBP"), OerClientConfig(appId, true))
val eodDate = DateTime(2011, 3, 13, 0, 0)
val tradeInYen = fx.convert(10000).to("JPY").eod(eodDate) // => convert 10000 GBP to JPY 
														  // according to the eod rate on the eodDate
```

### 3. Usage notes

#### LRU cache

The `lruCache` value determines the maximum number of values to keep in the LRU cache, which the Client will check prior to making an API lookup. To disable the LRU cache, set its size to zero, i.e. `lruCache = 0`.

#### From currency selection

A default "from currency" can be specified for all operations, using the `baseCurrency` argument to the `ForexConfig` object.

If this is not specified, all calls to `rate()` or `convert()` **must** specify the `fromCurrency` argument.

#### Constructor defaults

If not specified, the `eodCache` defaults to 60,000 entries. This is equivalent to around one year's worth of EOD currency rates for 165 currencies (165 * 365 = 60,225).

nowishCache = (165 * 164 / 2) = 13530 <- recommended size

eodCache = (165 * 164 / 2) * 30 = 405900 assuming 1 month <- suggested size

If not specified, the `nowishSecs` defaults to 300 seconds (5 minutes).

The 'getNearestDay' argurment is set to EodRoundDown by default to get the date prior to the specified date.

The 'homeCurrency' is set to USD by default, only Unlimited or Enterprise users can set it to other currencies.


## Implementation details

### End-of-day definition

The end of today is 00:00 on the next day

### Exchange rate lookup

When `.now` is specified, the **live** exchange rate available from Open Exchange Rates is used.

When `.nowish` is specified, a **cached** version of the **live** exchange rate is used, if the timestamp of that exchange rate is less than or equal to `nowishSecs` (see above) old. Otherwise a new lookup is performed.

When `.at(...)` is specified, the **latest end-of-day rate prior** to the datetime is used by default. Users can configure so that the rate on that specific date is used.
  
* What do we do if the EOD is not yet available? e.g. at 00:00:01?

When `.eod(...)` is specified, the end-of-day rate for the **specified day** is used. Any hour/minute/second/etc portion of the datetime is ignored.

### LRU cache

We recommend trying different LRU cache sizes to see what works best for you.

Please note that the LRU cache is **not** thread-safe ([see this note] [twitter-lru-cache]). Switch it off if you are working with threads.


## Copyright and license

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

[license]: http://www.apache.org/licenses/LICENSE-2.0

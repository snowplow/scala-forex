# Scala Forex [![Build Status](https://travis-ci.org/snowplow/scala-forex.png)](https://travis-ci.org/snowplow/scala-forex)

**PRE-ALPHA.**

## Introduction

Open Exchange Rates Scala Client is a high-performance Scala library for performing currency conversions using the [Open Exchange Rates API] [ore-api].

It includes a configurable LRU (Least Recently Used) cache to minimize calls to the Open Exchange Rates API; this makes the library usable in high-volume environments such as Hadoop and Storm.

This ORE Scala Client for foreign exchange is built on top of the [Open Exchange Rates Java Client] [ore-java], [Joda-Money] [joda-money] and [Joda-Time] [joda-time].

## Installation

First [sign up] [ore-signup] to Open Exchange Rates to get your App ID for API access.

Rest of section to come.

## Usage

The ORE Scala Client supports two types of usage:

1. Exchange rate lookups
2. Currency conversions

Both usage types support live, near-live or historical (end-of-day) exchange rates.

### 1. Rate lookup

#### Live rate

Lookup a live rate _(no cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(appId = "XXX", homeCurrency = "USD")
val usd2eur = fx.rate.to("EUR").now // => xxx
```

### Near-live rate

Lookup a near-live rate _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(appId = "XXX", homeCurrency = "USD", nowishSecs = 30)
val jpy2gbp = fx.rate("JPY").to("GBP").nowish // => xxx
```

### Latest-prior EOD rate

Lookup the latest EOD (end-of-date) rate prior to your event _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(appId = "XXX", lruCache = 4000)
val tradeDate = DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val usd2yen = fx.rate("USD").to("JPY").at(tradeDate) // => xxx
```

### Specific EOD rate

Lookup the EOD rate for a specific date _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(appId = "XXX", homeCurrency = "GBP", lruCache = 80000)
val eodDate = DateTime(2011, 3, 13, 0, 0)
val gbp2jpy = fx.rate.to("JPY").eod(eodDate) // => xxx
```

### 2. Currency conversion

#### Live rate

Conversion using the live exchange rate _(no cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(appId = "XXX", homeCurrency = "USD")
val priceInEuros = fx.convert(9.99).to("EUR").now // => xxx
```

#### Near-live rate

Conversion using a near-live exchange rate _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex

val fx = Forex(appId = "XXX", lruCache = 100000, nowishSecs = 30)
val priceInEuros = fx.convert(9.99, "USD").to("EUR").nowish // => xxx
```

#### Latest-prior EOD rate

Conversion using the latest EOD (end-of-date) rate prior to your event _(cacheing available)_:

```scala
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(appId = "XXX", lruCache = 4000)
val tradeDate = DateTime(2011, 3, 13, 11, 39, 27, 567, DateTimeZone.forID("America/New_York"))
val tradeInYen = fx.convert(10000, "GBP").to("JPY").at(tradeDate) // => xxx
```

#### Specific EOD rate

Conversion using the EOD rate for a specific date _(cacheing available)_:

```scala
...
import com.snowplowanalytics.forex.Forex
import org.joda.time.DateTime

val fx = Forex(appId = "XXX", homeCurrency = "GBP", lruCache = 80000)
val eodDate = DateTime(2011, 3, 13, 0, 0)
val tradeInYen = fx.convert(10000).to("JPY").eod(eodDate) // => xxx
```

### 3. Usage notes

#### LRU cache

The `lruCache` value determines the maximum number of values to keep in the LRU cache, which the Client will check prior to making an API lookup. To disable the LRU cache, set its size to zero, i.e. `lruCache = 0`.

#### From currency selection

A default "from currency" can be specified for all operations, using the `homeCurrency` argument to the `Forex` object.

If this is not specified, all calls to `rate()` or `convert()` **must** specify the `fromCurrency` argument.

#### Constructor defaults

If not specified, the `eodCache` defaults to 60,000 entries. This is equivalent to around one year's worth of EOD currency rates for 165 currencies (165 * 365 = 60,225).

nowishCache = (165 * 164 / 2) = 13530 <- recommended size

eodCache = (165 * 164 / 2) * 30 = 405900 assuming 1 month <- suggested size
*********************************


If not specified, the `nowishSecs` defaults to 300 seconds (5 minutes).

There is no default `homeCurrency`.

## Implementation details

### End-of-day definition

**TBC**

### Exchange rate lookup

When `.now` is specified, the **live** exchange rate available from Open Exchange Rates is used.

When `.nowish` is specified, a **cached** version of the **live** exchange rate is used, if the timestamp of that exchange rate is less than or equal to `nowishSecs` (see above) old. Otherwise a new lookup is performed.

When `.at(...)` is specified, the **latest end-of-day rate prior** to the datetime is used. **TBC:**

//by default, historical lookup gets the currency value on the previous day
//user can configure it to get that on the nearest day
  
* What do we do if the EOD is not yet available? e.g. at 00:00:01?
* Should we always use the last prior, or round forwards in time if the timestamp is closer to tomorrow?

When `.eod(...)` is specified, the end-of-day rate for the **specified day** is used. Any hour/minute/second/etc portion of the datetime is ignored.

### LRU cache

We recommend trying different LRU cache sizes to see what works best for you.

Please note that the LRU cache is **not** thread-safe ([see this note] [twitter-lru-cache]). Switch it off if you are working with threads.

## Roadmap

It could be nice to double the efficiency of the system by understanding that:

    EUR:USD EOD rate 21/09/2012 = 1 / (USD:EUR EOD rate)

So simply cache the first of those found, and calculate the other. Would need to investigate and check that this conversion was non-lossy.

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
[oer-java]: https://github.com/dneto/oer-java
[oer-signup]: https://openexchangerates.org/signup

[joda-money]: http://www.joda.org/joda-money/
[joda-time]: http://www.joda.org/joda-time/

[twitter-lru-cache]: http://twitter.github.com/commons/apidocs/com/twitter/common/util/caching/LRUCache.html

[license]: http://www.apache.org/licenses/LICENSE-2.0

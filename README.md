# Open Exchange Rates Scala Client

**NOT YET IMPLEMENTED.**

## Introduction

Open Exchange Rates Scala Client is a high-performance Scala library for performing currency conversions using the [Open Exchange Rates API] [ore-api]. It includes a configurable LRU (Least Recently Used) cache to minimize calls to the Open Exchange Rates API; this makes the library usable in high-volume environments such as Hadoop and Storm.

This Scala Client is built on top of the [Open Exchange Rates Java Client] [ore-java], [Joda-Money] [joda-money] and [Joda-Time] [joda-time].

## Installation

First [sign up] [ore-signup] to Open Exchange Rates to get your App ID for API access.

Rest of section to come.

## Usage

Here is a simple usage example for a live exchange rate:

```scala
import com.snowplowanalytics.ore.forex.Forex

val fx = Forex(appId = "XXX", homeCurrency = "USD", lruCache = 2000)
val priceInEuros = fx.convert(9.99).to("EUR").now
```

Here is a usage example for a historic exchange rate:

```scala
import com.snowplowanalytics.ore.forex.Forex
import org.joda.time.DateTime

val fx = Forex(appId = "XXX", lruCache = 2000)
val date = DateTime ny = new DateTime(2011, 3, 13, 2, 0, 0, 0, DateTimeZone.forID("America/New_York"));

val priceInEuros = fx.convert(9.99, "GBP").to("EUR").on(date)
```

## Conversion behaviour

When `convert...now` is specified, the **live** exchange rate available from Open Exchange Rates is used.

When `convert...on(...)` is specified, the most recent end-of-day rate still falling before the `on(...)` datetime is used.

**TBC: what do we do if the EOD is not yet available? e.g. at 00:00:01?**

## Roadmap

Nothing planned currently.

## Copyright and license

Copyright 2013 Snowplow Analytics Ltd.

Licensed under the [Apache License, Version 2.0] [license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[ore-api]: https://openexchangerates.org/
[ore-java]: https://github.com/dneto/oer-java
[ore-signup]: https://openexchangerates.org/signup

[joda-money]: http://www.joda.org/joda-money/
[joda-time]: http://www.joda.org/joda-time/

[license]: http://www.apache.org/licenses/LICENSE-2.0

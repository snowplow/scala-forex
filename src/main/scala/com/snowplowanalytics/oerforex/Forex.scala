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
package com.snowplowanalytics.oerforex

/**
 * Forex is a ???
 *
 */
// TODO: should homeCurrency be a String?
// TODO: should we ask what version of the API the user has access to?
// Because e.g. Enterprise is more powerful than Developer. Because
// Enterprise allows a homeCurrency to be set. Which means that
// conversions are easier.
// If a homecurrency can't be set, then for EUR -> GBP, I have to convert
// EUR -> USD -> GBP. Not very nice!
class Forex(appId: String, homeCurrency: String) {

  // Initialization code
  val oer = xxx

  // Public methods
  // TODO: let's start with an easy one
  def fxRateFromUsdTo(currency: String): XXX = {


  } // TODO: later we will update this to not assume USD!!
  // TODO: later we will move to "fluent builder" syntax e.g. fx.rate.to("EUR").now

}
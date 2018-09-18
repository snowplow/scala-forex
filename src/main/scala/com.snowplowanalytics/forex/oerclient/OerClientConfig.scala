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

/** OER-specific configuration */
case class OerClientConfig(
  /**
   * Register an account on https://openexchangerates.org to obtain your unique key
   */
  appId: String,
  accountLevel: AccountType // Account type of the user
) extends ForexClientConfig

/**
 * There are three types of accounts supported by OER API.
 * For scala-forex library, the main difference between Unlimited/Enterprise
 * and Developer users is that users with Unlimited/Enterprise accounts
 * can use the base currency for API requests, but this library will provide
 * automatic conversions between OER default base currencies(USD)
 * and user-defined base currencies. However this will increase calls to the API
 * and will slow down the performance.
 */
sealed trait AccountType
object DeveloperAccount extends AccountType
object EnterpriseAccount extends AccountType
object UnlimitedAccount extends AccountType

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
package oerclient

/**
 * OER error states from the HTTP requests
 * @param errorMessage - error message from OER API
 * @param errorType - specific error type
 */
case class OerResponseError(errorMessage: String, errorType: OerError)

/**
 * User defined error types
 */
sealed trait OerError

/**
 * Caused by invalid DateTime argument
 * i.e. either earlier than the earliest date OER service is available or later than currenct time
 */
object ResourcesNotAvailable extends OerError

/**
 * Currency not supported by API or
 * Joda Money or both
 */
object IllegalCurrency extends OerError

/**
 * Other possible error types e.g.
 * access permissions
 */
object OtherErrors extends OerError

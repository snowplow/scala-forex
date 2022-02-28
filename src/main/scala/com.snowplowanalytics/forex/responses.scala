/*
 * Copyright (c) 2013-2022 Snowplow Analytics Ltd. All rights reserved.
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

import scala.util.Try

import cats.instances.list._
import cats.syntax.functorFilter._
import io.circe._
import org.joda.money.CurrencyUnit

object responses {
  final case class OerResponse(rates: Map[CurrencyUnit, BigDecimal])

  // Encoder ignores Currencies that are not parsable by CurrencyUnit
  implicit val oerResponseDecoder: Decoder[OerResponse] = new Decoder[OerResponse] {
    override def apply(c: HCursor): Decoder.Result[OerResponse] =
      c.downField("rates").as[Map[String, BigDecimal]].map { map =>
        OerResponse(
          map
            .toList
            .mapFilter {
              case (key, value) => Try(CurrencyUnit.of(key)).toOption.map(_ -> value)
            }
            .toMap
        )
      }
  }
}

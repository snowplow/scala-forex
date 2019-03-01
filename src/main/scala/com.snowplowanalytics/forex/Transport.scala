/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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

import cats.Eval
import cats.free.Free
import cats.effect.{IO, Sync}
import hammock.{Hammock, HammockF, Method, Uri}
import hammock.marshalling._
import hammock.apache.ApacheInterpreter
import hammock.circe.implicits._
import io.circe.Decoder

import errors._
import responses._

trait Transport[F[_]] {

  /**
   * Main client logic for Request => Response function,
   * where Response is wrapped in tparam `F`
   * @param endpoint endpoint to interrogate
   * @param path path to request
   * @return extracted either error or exchanged rate wrapped in `F`
   */
  def receive(endpoint: String, path: String): F[Either[OerResponseError, OerResponse]]

}

object Transport {

  /**
   * Http Transport leveraging cats-effect's Sync.
   * @return a Sync Transport
   */
  def httpTransport[F[_]: Sync]: Transport[F] = new Transport[F] {

    implicit val interpreter = ApacheInterpreter[F]

    def receive(endpoint: String, path: String): F[Either[OerResponseError, OerResponse]] =
      buildRequest(endpoint, path)
        .exec[F]
  }

  /**
   * Unsafe http Transport to use in cases where you have to do side-effects (e.g. spark or beam).
   * @return an Eval Transport
   */
  def unsafeHttpTransport: Transport[Eval] = new Transport[Eval] {

    implicit val interpreter = ApacheInterpreter[IO]

    def receive(endpoint: String, path: String): Eval[Either[OerResponseError, OerResponse]] =
      Eval.later {
        buildRequest(endpoint, path)
          .exec[IO]
          .unsafeRunSync()
      }
  }

  implicit def eitherDecoder: Decoder[Either[OerResponseError, OerResponse]] =
    implicitly[Decoder[OerResponseError]].either(implicitly[Decoder[OerResponse]])

  private def buildRequest(
    endpoint: String,
    path: String
  ): Free[HammockF, Either[OerResponseError, OerResponse]] = {
    val authority = Uri.Authority(None, Uri.Host.Other(endpoint + path), None)
    val uri       = Uri(Some("http"), Some(authority))
    Hammock
      .request(Method.GET, uri, Map())
      .as[Either[OerResponseError, OerResponse]]
  }
}

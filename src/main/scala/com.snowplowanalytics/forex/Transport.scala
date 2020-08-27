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

import cats.{Eval, Id}
import cats.effect.Sync
import cats.syntax.either._
import io.circe.Decoder
import io.circe.parser._
import scalaj.http._

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
  implicit def httpTransport[F[_]: Sync]: Transport[F] =
    new Transport[F] {
      def receive(endpoint: String, path: String): F[Either[OerResponseError, OerResponse]] =
        Sync[F].delay(buildRequest(endpoint, path))
    }

  /**
    * Eval http Transport to use in cases where you have to do side-effects (e.g. spark or beam).
    * @return an Eval Transport
    */
  implicit def evalHttpTransport: Transport[Eval] =
    new Transport[Eval] {
      def receive(endpoint: String, path: String): Eval[Either[OerResponseError, OerResponse]] =
        Eval.later {
          buildRequest(endpoint, path)
        }
    }

  /**
    * Id http Transport to use in cases where you don't care about side-effects.
    * @return an Id Transport
    */
  implicit def idHttpTransport: Transport[Id] =
    new Transport[Id] {
      def receive(endpoint: String, path: String): Id[Either[OerResponseError, OerResponse]] =
        buildRequest(endpoint, path)
    }

  implicit def eitherDecoder: Decoder[Either[OerResponseError, OerResponse]] =
    implicitly[Decoder[OerResponseError]].either(implicitly[Decoder[OerResponse]])

  private def buildRequest(
    endpoint: String,
    path: String
  ): Either[OerResponseError, OerResponse] =
    for {
      response <- Http("http://" + endpoint + path).asString.body.asRight
      parsed <-
        parse(response).leftMap(e => OerResponseError(s"OER response is not JSON: ${e.getMessage}", OtherErrors))
      decoded <-
        parsed
          .as[Either[OerResponseError, OerResponse]]
          .leftMap(_ => OerResponseError(s"OER response couldn't be decoded", OtherErrors))
      res <- decoded
    } yield res
}

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

import java.time.ZonedDateTime

import cats.Eval
import cats.effect.Sync

trait ZonedClock[F[_]] {
  def currentTime: F[ZonedDateTime]
}

object ZonedClock {
  implicit def zonedClock[F[_]: Sync]: ZonedClock[F] = new ZonedClock[F] {
    def currentTime: F[ZonedDateTime] = Sync[F].delay(ZonedDateTime.now)
  }

  implicit def evalZonedClock: ZonedClock[Eval] = new ZonedClock[Eval] {
    def currentTime: Eval[ZonedDateTime] = Eval.later(ZonedDateTime.now)
  }

  implicit def idZonedClock: ZonedClock[Id] = new ZonedClock[Id] {
    def currentTime: Id[ZonedDateTime] = ZonedDateTime.now
  }
}

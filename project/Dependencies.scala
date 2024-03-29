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
import sbt._
import Keys._

object Dependencies {

  object V {
    // Java
    val jodaMoney   = "1.0.1"
    val jodaConvert = "2.2.0"

    // Scala
    val catsEffect = "3.3.5"
    val circe      = "0.14.3"
    val lruMap     = "0.6.0"
    val scalaj     = "2.4.2"

    // Scala (test only)
    val specs2 = "4.13.2"
  }

  object Libraries {
    // Java
    val jodaMoney   = "org.joda" % "joda-money"   % V.jodaMoney
    val jodaConvert = "org.joda" % "joda-convert" % V.jodaConvert

    // Scala
    val catsEffect  = "org.typelevel"         %% "cats-effect"   % V.catsEffect
    val circeParser = "io.circe"              %% "circe-parser"  % V.circe
    val lruMap      = "com.snowplowanalytics" %% "scala-lru-map" % V.lruMap
    val scalaj      = "org.scalaj"            %% "scalaj-http"   % V.scalaj

    // Scala (test only)
    val specs2Core = "org.specs2" %% "specs2-core" % V.specs2 % "test"
    val specs2Mock = "org.specs2" %% "specs2-mock" % V.specs2 % "test"
  }
}

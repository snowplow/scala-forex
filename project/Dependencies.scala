/*
 * Copyright (c) 2013 Snowplow Analytics Ltd. All rights reserved.
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

object Dependencies {

  val resolutionRepos = Seq(
    // For scala-util
    "Snowplow Analytics Maven repo" at "http://maven.snplow.com/releases/"
  )

  object V {
    // Java
    val jodaTime   = "2.3"
    val jodaMoney  = "0.9"
    // val awsSdk  = "1.6.4" Only available on Amazon Kinesis private beta, so this is an unmanaged lib
    // Scala
    val scalaUtil  = "0.1.0"
    // Scala (test only)
    val specs2     = "2.3.4"
  }

  object Libraries {
    // Java
    val jodaTime    = "joda-time"                  % "joda-time"        % V.jodaTime
    val jodaMoney   = "org.joda"                   % "joda-money"       % V.jodaMoney
    // Scala
    val scalaUtil   = "com.snowplowanalytics"      %  "scala-util"      % V.scalaUtil
    // Scala (test only)
    val specs2      = "org.specs2"                 %% "specs2"          % V.specs2     % "test"
  }
}
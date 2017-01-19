/*
 * Copyright (c) 2013-2015 Snowplow Analytics Ltd. All rights reserved.
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

  val resolutionRepos = Seq(
    // For scala-util
    "Snowplow Analytics" at "http://maven.snplow.com/releases/",
    // For Twitter's LRU cache
    "Twitter Maven Repo" at "http://maven.twttr.com/",
    "Sonatype" at "https://oss.sonatype.org/content/repositories/releases"
  )

  object V {
    // Java
    val jodaTime    = "2.3"
    val jodaMoney   = "0.9"
    val jodaConvert = "1.2"
    val jackson     = "1.9.7"

    // Scala
    val scalaUtil = "0.1.0"
    val collUtil = "6.34.0"

    // Java (test only)
    val mockito     = "1.9.5"

    // Scala (test only)
    val specs2 = "2.3.13"
  }

  object Libraries {
    // Java
    val jodaTime    = "joda-time"             % "joda-time"          % V.jodaTime
    val jodaMoney   = "org.joda"              % "joda-money"         % V.jodaMoney
    val jodaConvert = "org.joda"              % "joda-convert"       % V.jodaConvert
    val jackson     = "org.codehaus.jackson"  % "jackson-mapper-asl" % V.jackson

    // Scala
    val scalaUtil   = "com.snowplowanalytics" %% "scala-util"        % V.scalaUtil
    val collUtil    = "com.twitter"           %% "util-collection"   % V.collUtil

    // Java (test only)
    val mockito     = "org.mockito"           %  "mockito-all"       % V.mockito       % "test"

    // Scala (test only)
    val specs2      = "org.specs2"            %% "specs2"            % V.specs2        % "test"
  }
}

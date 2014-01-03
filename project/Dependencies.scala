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
import sbt._
import Keys._

object Dependencies {

  val resolutionRepos = Seq(
    // For scala-util
    "Snowplow Analytics Maven repo" at "http://maven.snplow.com/releases/",
    // For Twitter's LRU cache
    "Twitter Maven Repo" at "http://maven.twttr.com/"
  )

  object V {
    // Java
    val jodaTime    = "2.3"
    val jodaMoney   = "0.9"
    val jodaConvert = "1.2"
    val jackson     = "1.9.7" // Needed by java-oer and not contained in /lib/oer-java-0.1.0.jar
    // val awsSdk  = "1.6.4" Only available on Amazon Kinesis private beta, so this is an unmanaged lib
    // Scala
    val collUtilOld = "5.3.10"
    val collUtil    = "6.3.4"
    val scalaUtil   = "0.1.0"
    // Scala (test only)
    val specs2Old   = "1.12.4.1"
    val specs2      = "2.3.7"
  }

  object Libraries {
    // Java
    val jodaTime    = "joda-time"                  % "joda-time"          % V.jodaTime
    val jodaMoney   = "org.joda"                   % "joda-money"         % V.jodaMoney
    val jackson     = "org.codehaus.jackson"       % "jackson-mapper-asl" % V.jackson
    val jodaConvert = "org.joda"                   % "joda-convert"       % V.jodaConvert
    // Scala
    val collUtilOld = "com.twitter"                %  "util-collection"   % V.collUtilOld
    val collUtil    = "com.twitter"                %% "util-collection"   % V.collUtil
    val scalaUtil   = "com.snowplowanalytics"      %  "scala-util"        % V.scalaUtil
    // Scala (test only)
    val specs2Old   = "org.specs2"                 %% "specs2"            % V.specs2Old    % "test"
    val specs2      = "org.specs2"                 %% "specs2"            % V.specs2       % "test"
  }

  def onVersion[A](all: Seq[A] = Seq(), on292: => Seq[A] = Seq(), on210: => Seq[A] = Seq()) =
    scalaVersion(v => all ++ (if (v.contains("2.10")) on210 else on292))
}
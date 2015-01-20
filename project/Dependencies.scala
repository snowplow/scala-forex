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
    val scalaUtil   = "0.1.0"
    object collUtil {
      val _29       = "5.3.10"
      val _210      = "6.3.4"
      val _211      = "6.23.0"
    }

    // Java (test only)
    val mockito     = "1.9.5"

    // Scala (test only)
    object specs2 {
      val _29       = "1.12.4.1"
      val _210      = "2.3.13"
      val _211      = "2.3.13"
    }
  }

  object Libraries {
    // Java
    val jodaTime    = "joda-time"                  % "joda-time"          % V.jodaTime
    val jodaMoney   = "org.joda"                   % "joda-money"         % V.jodaMoney
    val jodaConvert = "org.joda"                   % "joda-convert"       % V.jodaConvert
    val jackson     = "org.codehaus.jackson"       % "jackson-mapper-asl" % V.jackson

    // Scala
    val scalaUtil   = "com.snowplowanalytics"      %  "scala-util"        % V.scalaUtil
    object collUtil {
      val _29       = "com.twitter"                % "util-collection"    % V.collUtil._29
      val _210      = "com.twitter"                %% "util-collection"   % V.collUtil._210
      val _211      = "com.twitter"                %% "util-collection"   % V.collUtil._211
    }

    // Java (test only)
    val mockito     = "org.mockito"                %  "mockito-all"       % V.mockito            % "test"

    // Scala (test only)
    object specs2 {
      val _29       = "org.specs2"                 %% "specs2"            % V.specs2._29         % "test"
      val _210      = "org.specs2"                 %% "specs2"            % V.specs2._210        % "test"
      val _211      = "org.specs2"                 %% "specs2"            % V.specs2._211        % "test"
    }
  }

  def onVersion[A](all: Seq[A] = Seq(), on29: => Seq[A] = Seq(), on210: => Seq[A] = Seq(), on211: => Seq[A] = Seq()) =
    scalaVersion(v => all ++ (if (v.contains("2.9.")) {
      on29
    } else if (v.contains("2.10.")) {
      on210
    } else {
      on211
    }))
}
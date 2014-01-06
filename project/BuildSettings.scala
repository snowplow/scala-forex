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

object BuildSettings {

  // Basic settings for our app
  lazy val basicSettings = Seq[Setting[_]](
    organization          :=  "Snowplow Analytics Ltd",
    version               :=  "0.0.1",
    description           :=  "High-performance Scala library for performing currency conversions using Open Exchange Rates",
    scalaVersion          :=  "2.10.0",
    crossScalaVersions    :=  Seq("2.9.2", "2.9.3", "2.10.0", "2.10.1"), 
    scalacOptions         :=  Seq("-deprecation", "-encoding", "utf8"),
    resolvers             ++= Dependencies.resolutionRepos
  )

  lazy val buildSettings = basicSettings
}

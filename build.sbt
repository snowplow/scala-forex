/*
 * Copyright (c) 2013-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and
 * limitations there under.
 */

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin, GhpagesPlugin)
  .settings(
    name := "scala-forex",
    version := "1.0.0",
    description := "High-performance Scala library for performing currency conversions using Open Exchange Rates"
  )
  .settings(BuildSettings.buildSettings)
  .settings(BuildSettings.publishSettings)
  .settings(BuildSettings.docsSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Libraries.jodaConvert,
      Dependencies.Libraries.jodaMoney,
      Dependencies.Libraries.catsEffect,
      Dependencies.Libraries.circeParser,
      Dependencies.Libraries.lruMap,
      Dependencies.Libraries.scalaj,
      Dependencies.Libraries.specs2Core,
      Dependencies.Libraries.specs2Mock
    )
  )

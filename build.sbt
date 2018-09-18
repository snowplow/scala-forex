/*
 * Copyright (c) 2013-2017 Snowplow Analytics Ltd. All rights reserved.
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

lazy val root = project.in(file("."))
  .settings(
    name        := "scala-forex",
    version     := "0.5.0",
    description := "High-performance Scala library for performing currency conversions using Open Exchange Rates"
  )
  .settings(BuildSettings.buildSettings)
  .settings(BuildSettings.publishSettings)
  .settings(BuildSettings.formattingSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Libraries.jodaConvert,
      Dependencies.Libraries.jodaMoney,
      Dependencies.Libraries.jackson,
      Dependencies.Libraries.collUtil,
      Dependencies.Libraries.specs2Core,
      Dependencies.Libraries.specs2Mock
    )
  )

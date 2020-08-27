/*
 * Copyright (c) 2013-2019 Snowplow Analytics Ltd. All rights reserved.
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

// SBT
import sbt._
import Keys._

// Bintray plugin
import bintray.BintrayPlugin._
import bintray.BintrayKeys._

// Docs
import sbtunidoc.ScalaUnidocPlugin.autoImport._
import com.typesafe.sbt.site.SitePlugin.autoImport._
import com.typesafe.sbt.SbtGit.GitKeys._

object BuildSettings {

  // Basic settings for our app
  lazy val buildSettings = Seq[Setting[_]](
    organization := "com.snowplowanalytics",
    scalaVersion := "2.12.8"
  )

  // Publish settings
  lazy val publishSettings = bintraySettings ++ Seq(
    publishMavenStyle := true,
    publishArtifact := true,
    publishArtifact in Test := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    bintrayOrganization := Some("snowplow"),
    bintrayRepository := "snowplow-maven",
    pomIncludeRepository := { _ => false },
    homepage := Some(url("http://snowplowanalytics.com")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/snowplow/scala-forex"), "scm:git@github.com:snowplow/scala-forex.git")
    ),
    pomExtra := (<developers>
        <developer>
          <name>Snowplow Analytics Ltd</name>
          <email>support@snowplowanalytics.com</email>
          <organization>Snowplow Analytics Ltd</organization>
          <organizationUrl>http://snowplowanalytics.com</organizationUrl>
        </developer>
      </developers>)
  )

  lazy val docsSettings = Seq(
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    gitRemoteRepo := "https://github.com/snowplow/scala-forex.git",
    siteSubdirName := ""
  )
}

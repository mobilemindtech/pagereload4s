
lazy val scala3 = "3.9.0"

scalaVersion     := scala3
version          := "0.4.0"
organization     := "br.com.mobilemind"
organizationName := "Mobild Mind"
organizationHomepage := Some(url("https://www.mobilemind.com.br"))
description := "Page Reload for Scala"
//sonatypeCredentialHost := "s01.oss.sonatype.org"
isSnapshot := true

lazy val root = (project in file("."))
  .enablePlugins()
  .settings(
    name := "pagereload4s",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "io.undertow" % "undertow-core" % "2.4.4.Final"
    )
)
  .settings(addSbtPlugin("org.scala-js" %% "sbt-scalajs" % "1.22.0"))

lazy val scala3 = "3.8.4"

scalaVersion     := scala3
version          := "0.3.0"
organization     := "br.com.mobilemind"
organizationName := "Mobild Mind"
organizationHomepage := Some(url("https://www.mobilemind.com.br"))
description := "Live Reload for ScalaJS"
//sonatypeCredentialHost := "s01.oss.sonatype.org"


lazy val root = (project in file("."))
  .settings(
    name := "reload4scalajs",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "cask" % "0.11.3",
      "com.lihaoyi" %% "upickle" % "4.4.3",
    ),
    //credentials ++= Seq(
    //  Credentials(Path.userHome / ".sbt" / "sonatype_credentials"),
    //  Credentials(Path.userHome / ".sbt" / "sonatype_gpg")
    //)
)
  .settings(addSbtPlugin("org.scala-js" %% "sbt-scalajs" % "1.22.0"))
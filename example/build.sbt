name := "example"
scalaVersion := "3.8.4"

lazy val app = (project in file("."))
  .enablePlugins(ScalaJSPlugin, Reload4ScalaJSPlugin, CopyJSOnCompilePlugin)
  .enablePlugins()
  .settings(
    name := "example",
    scalaJSUseMainModuleInitializer := true,
    livereloadPublic := Some(baseDirectory.value / "public"),
    copyJsToFile := baseDirectory.value / "public" / "assets" / "js" / "main.js",
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-dom" % "2.8.1"
    )
  )
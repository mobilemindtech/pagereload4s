/*
// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "br.com.mobilemind"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// Open-source license of your choice
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

// Where is the source code hosted: GitHub or GitLab?
import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("mobilemindtec", "scalajs-livereload", "ricardo@mobilemind.com.br"))

// or if you want to set these fields manually
homepage := Some(url("https://github.com/mobilemindtec/scalajs-livereload"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/mobilemindtec/scalajs-livereload"),
    "scm:git@github.com:mobilemindtec/scalajs-livereload.git"
  )
)
developers := List(
  Developer(
    id="ricardobocchi",
    name="Ricardo Bocchi",
    email="ricardo@mobilemind.com.br",
    url=url("https://www.mobilemind.com.br"))
)

publishTo := sonatypePublishToBundle.value
*/

package br.com.mobilemind.r4sjs.plugin

import sbt.*
import Keys.*
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

// this does not adjust the content of the source map at all!
object CopyJSOnCompilePlugin extends AutoPlugin {

  override def requires = ScalaJSPlugin
  object autoImport {
    val copyJsToFile = SettingKey[File]("copyJsToFile", "Absolute file path to copy js after compile")
    val performCopyJs     = TaskKey[Unit]("performCopyJs", "Performs the JS copy")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[Task[Unit]]] = Seq(
    performCopyJs := copyJSTask.value,
    fastOptJS / performCopyJs := performCopyJs.triggeredBy(Compile / fastOptJS).value,
    fullOptJS / performCopyJs := performCopyJs.triggeredBy(Compile / fullOptJS).value
  )
  //define inline in autoImport via `copyJSTask := {` or separately like this
  private def copyJSTask = Def.task {
    val logger = streams.value.log
    val odir = copyJsToFile.value
    val src = (Compile / scalaJSLinkedFile).value.data
    val isJsFileName = odir.getCanonicalPath.endsWith(".js")
    val fileName = if (isJsFileName) odir.name else src.name
    val destPath = if (isJsFileName) odir.getParentFile else odir

    logger.info(s"Copying artifacts [js,map] from ${src.getParent} to [${destPath.getCanonicalPath}]")

    IO.copy(
      Seq(
        (src, destPath / fileName),
        (file(src.getCanonicalPath + ".map"), destPath / (fileName + ".map"))
      ),
      CopyOptions(
        overwrite = true,
        preserveLastModified = true,
        preserveExecutable = true
      )
    )
  }
}
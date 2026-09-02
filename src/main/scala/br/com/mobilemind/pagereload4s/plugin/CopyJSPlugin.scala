package br.com.mobilemind.pagereload4s.plugin

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.scalajs.linker.interface.Report
import sbt.*
import sbt.Keys.*

import java.io.File
import scala.language.postfixOps


object CopyJSPlugin extends AutoPlugin {
  override def requires = ScalaJSPlugin
  override def trigger = allRequirements

  object autoImport {
    val copyJSToPath = SettingKey[File]("copyJSToPath", "Path to copy")
  }

  import autoImport.*

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    Compile / fastLinkJS := Def.uncached {
      val report = (Compile / fastLinkJS).value
      val src = (Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
      val dest = copyJSToPath.value
      val fileName = s"${moduleName.value}.js"
      IO.copy(
        Seq(
          (src / "main.js", dest / fileName),
          (src / "main.js.map", dest / s"$fileName.map")
        ),
        CopyOptions(
          overwrite = true,
          preserveLastModified = true,
          preserveExecutable = true
        )
      )

      report
    },
    Compile / fullLinkJS := Def.uncached {
      val report = (Compile / fullLinkJS).value
      val src = (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
      val dest = copyJSToPath.value
      val fileName = s"${moduleName.value}.js"
      IO.copy(
        Seq(
          (src / "main.js", dest / fileName),
          (src / "main.js.map", dest / s"$fileName.map")
        ),
        CopyOptions(
          overwrite = true,
          preserveLastModified = true,
          preserveExecutable = true
        )
      )

      report
    }
  )
}
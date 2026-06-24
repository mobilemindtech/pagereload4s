package br.com.mobilemind.r4sjs.plugin

import br.com.mobilemind.r4sjs.core.{FileWatcher, Server, ServerConfigs, WsSession}
import sbt.*
import sbt.Keys.*

import scala.jdk.CollectionConverters.*
import java.io.File
import java.nio.file.{FileSystems, Files}
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.sys.process.*

object WatcherUtil {

  def watch(extensions: List[String],
            debug: Boolean,
            logger: CustomLogger,
            target: File,
            dest: Option[File],
            notify: Boolean): Unit = {
    FileWatcher.create(extensions, debug).start(target, dest) {
      (_, _) => if(notify) Server.notify(logger)
    } match {
      case Failure(ex) => logger.error(ex.getMessage)
      case _ => logger.info(s"watch `${target.getAbsolutePath}` successful started, copy to: ${dest.map(_.getAbsolutePath).getOrElse("no copy")}")
    }
  }

  def getAllDirs(f: File): List[File] = {
    val dir = FileSystems.getDefault.getPath(f.getAbsolutePath)
    val dirs = Files
      .list(dir)
      .toList
      .asScala
      .map(_.toFile)
      .filter(_.isDirectory)
      .toList
    dirs ::: dirs.flatMap(x => getAllDirs(x))
  }
}

object Reload4ScalaJSPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val livereloadWatchTarget = SettingKey[Option[File]]("livereloadWatchTarget", "Target path to watch changes")
    val livereloadCopyJSTo = SettingKey[Option[File]]("livereloadCopyJSTo", "Destination folder to copy js after compilation")
    val livereloadPublic = SettingKey[Option[File]]("livereloadPublic", "Static dir to serve")
    val livereloadPublicJS = SettingKey[Option[String]]("livereloadPublicJS", "JS folder to serve. Default value is assets/js")
    val livereloadWatchPublic = SettingKey[Option[Boolean]]("livereloadWatchPublic", "If should watch dist folder. Default value is true if livereloadPublic is defined, otherwise false. If true, livereloadPublic must be defined.")
    val livereloadDebug = SettingKey[Option[Boolean]]("livereloadDebug", "Run plugin on debug mode")
    val livereloadServerPort = SettingKey[Option[Int]]("livereloadServerPort", "Http server port. Default value is 10101.")
    val livereloadExtensions = SettingKey[Option[List[String]]]("livereloadExtensions", "file extensions to watch")
    val liveReloadUrl = SettingKey[Option[String]]("liveReloadUrl", "External URL to reload when JS change")
    val livereloadServe = taskKey[Unit]("Start http server")
    val livereloadWatch = taskKey[Unit]("Start file watcher")
    val livereloadStart = taskKey[Unit]("Start the reload plugin")
    val livereloadStop = taskKey[Unit]("Stop the reload plugin")
    val npmInstall = taskKey[Unit]("Run npm install")
    val defaultExtensions: Seq[String] = List("js", "map", "css", "jpg", "jpeg", "png", "ico", "html")
  }

  import autoImport.*

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    livereloadWatchTarget := None,
    livereloadCopyJSTo := None,
    livereloadPublic := None,
    livereloadPublicJS := None,
    livereloadServerPort := None,
    livereloadDebug := None,
    livereloadExtensions := None,
    livereloadWatchPublic := None,
    liveReloadUrl := None,
    npmInstall := {
      val s: TaskStreams = streams.value
      val shell: Seq[String] = if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq("bash", "-c")
      val install: Seq[String] = shell :+ "npm install"
      val result = install.!
      if(result == 0){
        s.log.success("frontend build successful!")
      }else{
        s.log.success("error run npm install")
      }
    },
    livereloadServe := Def.uncached { 
      val s = streams.value
      Server.start(
        CustomLogger(s.log),
        ServerConfigs(livereloadServerPort.value, livereloadPublic.value, liveReloadUrl.value))
    },
    livereloadWatch := Def.uncached {
      val targetName = s"${name.value}-fastopt"
      val target = new File((Compile / crossTarget).value, targetName)
      val targetPath = livereloadWatchTarget.value.getOrElse(target)
      val extensions_ = livereloadExtensions.value.getOrElse(defaultExtensions)
      val debug_ = livereloadDebug.value.getOrElse(false)
      val logger = CustomLogger(streams.value.log)

      FileWatcher.setLogger(logger)

      // set copyTo to copy destination on change target files
      livereloadCopyJSTo.value.foreach {
        f => {
          WatcherUtil.watch(
            extensions_.toList,
            debug_, logger,
            targetPath,
            Some(f),
            notify = false) // dist.value.isEmpty
        }
      }

      // set dist to copy destination on change target files
      livereloadPublic.value.foreach {
        f => {
          val destination = new File(f, livereloadPublicJS.value.getOrElse("assets/js"))
          WatcherUtil.watch(
            extensions_.toList,
            debug_,
            logger,
            targetPath,
            Some(destination),
            notify = false)
        }
      }

      // watch dist path to notify on changes
      livereloadWatchPublic.value.orElse(Some(livereloadPublic.value.nonEmpty)).filter(x => x).flatMap(_ => livereloadPublic.value).foreach {
        distTarget => {
          val dirs = distTarget :: WatcherUtil.getAllDirs(distTarget)
          dirs.foreach {
            f => WatcherUtil.watch(
              extensions_.toList, 
              debug_, 
              logger, 
              f, 
              None, 
              notify = true)
          }
        }
      }
    },
    Compile / compile := Def.uncached {
      val compileValue = (Compile / compile).value
      val streamValue = streams.value
      val logger = CustomLogger(streamValue.log)
      FileWatcher.setLogger(logger)
      Server.setLogger(logger)
      val watchingDist = 
        livereloadPublic.value.isDefined && livereloadWatchPublic.value.getOrElse(true)
      if(!watchingDist) Server.notify(logger)
      compileValue
    },
    (Global / onUnload) := {
      val previousUnload = (Global / onUnload).value
      previousUnload.compose { state =>
        val extracted = Project.extract(state)
        val (nextState, _) = extracted.runTask(livereloadStop, state)
        nextState
      }
    },
    livereloadStart := livereloadServe.dependsOn(Def.task(livereloadWatch.value)).value,
    livereloadStop := Def.uncached {
      val logger = CustomLogger(streams.value.log)
      FileWatcher.setLogger(logger)
      Server.setLogger(logger)
      logger.info("stop server")
      FileWatcher.stop()
      logger.info("stop file watch")
      Server.stop()
    }
  )
}
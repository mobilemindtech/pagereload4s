package br.com.mobilemind.r4sjs.core

import br.com.mobilemind.r4sjs.plugin.CustomLogger

import java.io.{File, FileWriter, IOException}
import java.nio.file.StandardWatchEventKinds.{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY}
import java.nio.file.*
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.{Timer, TimerTask}
import scala.util.{Failure, Success, Try}
import scala.collection.mutable

object FileWatcher {
	private var logger: Option[CustomLogger]  = None
	private val watchers: mutable.ListBuffer[FileWatcher] = new mutable.ListBuffer

	private def log(text: String): Unit = logger.foreach(_.info(text))

	def setLogger(l: CustomLogger): Unit = logger = Some(l)

	def stop(): Unit = {
		logger.foreach(_.info(s"stop [${watchers.length}] watchers"))
		watchers.foreach(_.stop())
		watchers.clear()
	}

	def create(extensions: List[String] = List(), debug: Boolean = false): FileWatcher = {
		new FileWatcher(extensions, debug)
	}
}

class FileWatcher(val extensions: List[String], debug: Boolean = false){

	import FileWatcher.*

	private var timer: Option[Timer] = None
	private var running: Boolean = true

	watchers.append(this)

	def stop(): Unit = {
		running = false
		timer.foreach(_.cancel())
	}

	def start(targetPath: File, copyToPath: Option[File] = None)(fn: (String, Path) => Unit): Try[Unit] = {
			Try(tryCreateIfNeed(targetPath)) match {
				case Success(_) => copyToPath match {
					case Some(toPath) => Success { startTimer { watch(targetPath, toPath)(fn) } };
					case None => Success { startTimer { watch(targetPath)(fn) } }
				}
				case Failure(exception) => Failure(exception)
			}
	}

	private def startTimer(fn: => Unit): Unit = {
		val t = new Timer
		t.schedule(new TimerTask {
			override def run(): Unit = {
				fn
			}
		}, 0)
		timer = Some(t)
	}


	private def watch(fromPath: File, toPath: File)(cb: (String, Path) => Unit): Unit = {
		watch(fromPath) {
			(fileName, from) => {
				val to = Paths.get(toPath.getAbsolutePath, fileName)
				if(debug) log(s"new file version of $fileName")
				Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
				cb(fileName, from)
			}
		}
	}

	private def watch(fromPath: File)(cb: (String, Path) => Unit): Unit = {
		val watcher = FileSystems.getDefault.newWatchService
		Paths.get(fromPath.getAbsolutePath).register(watcher,
			ENTRY_CREATE,
			ENTRY_DELETE,
			ENTRY_MODIFY);

		if(debug) {
			logger.foreach(_.info(s"file watch listen: ${fromPath.getAbsolutePath}, ${this.hashCode()}"))
			logWriter(fromPath, s"file watch listen: ${fromPath.getAbsolutePath}")
		}

		try
			watchLoop(watcher, fromPath, cb)
		finally
			watcher.close()

		if (debug)
			logWriter(fromPath, s"file watch done to listen: ${fromPath.getAbsolutePath}")

	}

	private def watchLoop(watcher: WatchService, path: File, cb: (String, Path) => Unit): Unit = {
		while (running) {
			val k = watcher.take()

			if(running) {
				val events = k.pollEvents().toArray
				for (event <- events) {
					val ev = event.asInstanceOf[WatchEvent[Path]]
					val fileName = ev.context().toFile.getName

					val ok = extensions.exists(ext => fileName.endsWith(s".${ext}"))
					if (debug)
						logWriter(path, s"file changed: ${fileName}, extensions ${extensions.mkString(",")}, check: $ok")

					if (ok)
						cb(fileName, Paths.get(path.getAbsolutePath, fileName))
				}
				k.reset()
			}
		}
	}

	private def tryCreateIfNeed(target: File) =
		if (!target.exists() && !target.mkdirs())
			Failure(new IOException(s"can't create path ${target.getAbsolutePath}"))
		else Success(true)

	private def logWriter(path: File, text: String): Unit = {
		val target = new File("./target")
		val logFile = new File(target, s"livereload.log")
		val writer = new FileWriter(logFile, true)
		writer.write(s"${LocalDateTime.now().toString}: $text\n")
		writer.flush()
		writer.close()
	}
}

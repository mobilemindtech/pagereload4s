package br.com.mobilemind.r4sjs.core

import br.com.mobilemind.r4sjs.plugin.CustomLogger
import cask.Ws
import cask.endpoints.WsChannelActor
import cask.model.Response
import io.undertow.{Undertow, UndertowOptions}
import upickle.default.*

import java.io
import java.io.{BufferedReader, File, FileReader, InputStreamReader}
import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.util.{Success, Using}


case class ServerConfigs(port: Option[Int]  = None,
												 www: Option[File] = None,
												 reloadUrl: Option[String] = None)

class myStaticResources(path: String, headers: Seq[(String, String)]) extends
	cask.staticResources(path, classOf[cask.staticResources].getClassLoader, headers)

case class Asset(data: String, contentType: String, status: Int = 200)

object WsSession {

	private val sessions = TrieMap[WsChannelActor, cask.WsActor]()

	def count: Int = sessions.size

	def newSession(id: String)(using ctx: castor.Context, log: cask.Logger): cask.WsHandler = {
		cask.WsHandler { channel =>
			val actor = newSession(id, channel)
			channel.send(createEvent("alive"))
			sessions += (channel -> actor)
			actor
		}
	}

	private def newSession(id: String, channel: WsChannelActor)(using ctx: castor.Context, log: cask.Logger): cask.WsActor = {

		cask.WsActor {
				case cask.Ws.Text("") =>
					channel.send(cask.Ws.Close())
					removeSession(channel)
				case cask.Ws.Text(data) =>
					channel.send(cask.Ws.Text(id + " " + data))
				case cask.Ws.ChannelClosed() =>
					removeSession(channel)
					//if (idx > -1) sessions.remove(idx)
				case _: cask.Ws.Close =>
					removeSession(channel)
			}
	}

	private def removeSession(channel: WsChannelActor) =
		sessions -= channel

	def notify(logger: CustomLogger): Unit = {
		@tailrec
		def sendAndClose(activeSessions: Seq[(WsChannelActor, cask.WsActor)]): Unit = {
			activeSessions match
				case Nil => ()
				case (channel, _) :: xs =>
					//println (s"LiveReload: send reload to client")
					channel.send (createEvent("reload") )
					channel.send (Ws.Close () )
					removeSession (channel)
					sendAndClose(xs)
		}
		sendAndClose(sessions.toList)
	}

	private def createEvent(s: String) =
		Ws.Text(write(Map("event" -> s)))

	def stop(): Unit =
		sessions.clear()
}

class AppController(dist: => Option[File], reloadUrl: Option[String])
									 (using ctx: castor.Context, log: cask.Logger) extends cask.MainRoutes {

	private def readIndexHtml(req: cask.Request, path: File): Option[String] = {
		val indexFile = new File(path, s"index.html")
		if indexFile.exists() then
			Using(new BufferedReader(new FileReader(indexFile))) { reader =>
				val html = reader.lines()
					.toList
					.asScala
					.mkString("\n")

				if html.contains("/livereload.js")
				then html
				else
					val port = req.exchange.getHostPort
					val jsUrl = reloadUrl.getOrElse(s"http://localhost:$port/js/livereload.js")
					html.replace("</body>", s"""<script src="$jsUrl"></script>\n</body>""")

			}.map(Some(_)).getOrElse(None)
		else None
	}

	private def readAsset(assetPath: File, assetParts: String): Asset = {
		val file = new File(assetPath, s"/assets/${assetParts}")
		val assetNotFound = Asset(s"file ${file.getAbsolutePath} not found", "text/plain", 404)
		if file.exists() then
			val fileName = file.getName
			val ext = fileName.split("\\.").toList.last
			val contentType = MimeTypes.values.getOrElse(s".$ext", MimeTypes.defaultMimeType)
			Using(new BufferedReader(new FileReader(file))) { reader =>
				val data = reader.lines().toList.asScala.mkString("\n")
				Asset(data, contentType)
			}.getOrElse(assetNotFound)
		else assetNotFound
	}

	@cask.get("/healthcheck")
	def healthcheck() = s"live reload is alive"

	@cask.get("/")
	def index(req: cask.Request): Response[String] = {
		val resp = dist match {
			case Some(file) => readIndexHtml(req, file).getOrElse("index.html not fond")
			case _ => "live reload is alive"
		}
		cask.Response(resp, 200, Seq("Content-Type" -> "text/html"))
	}

	@cask.get("/dist")
	def getDist() = {
		dist.map(_.getAbsolutePath).getOrElse("empty")
	}

	@cask.get("/assets", subpath = true)
	def assets(req: cask.Request) = {
		dist match {
			case Some(f) =>
				val assetParts = req.remainingPathSegments.mkString("/")
				val Asset(data, contentType, status)= readAsset(f, assetParts)
				cask.Response(data, status, Seq("Content-Type" -> contentType))
			case _ => cask.Response("not found", 404)
		}
	}

	@myStaticResources("/demo", headers = Seq("Content-Type" -> "text/html"))
	def demo() = "public/html/index.html"

	@cask.get("/js/livereload.js", subpath = true)
	def livereloadJS(req: cask.Request) = {
		val res = classOf[cask.staticResources]
			.getClassLoader
			.getResourceAsStream("public/js/livereload.js")
		Using(new BufferedReader(new InputStreamReader(res))) { reader =>
			val port = req.exchange.getHostPort
			reader
				.lines()
				.toList
				.asScala
				.map(_.replace("__PORT__", port.toString))
				.map(_.replace("__RELOAD_URL__", reloadUrl.getOrElse(s"http://localhost:$port")))
				.mkString("\n")
		} match
			case Success(data) => cask.Response(data, 200, Seq("Content-Type" -> "text/javascript"))
			case _ => cask.Response("not found", 404)
	}

	@cask.websocket("/ws")
	def ws(): cask.WebsocketResult = WsSession.newSession("userName")

	initialize()
}

object Server extends cask.Main {

	private var logger: Option[CustomLogger] = None
	private var server : Option[Undertow] = None
	private var configs: Option[ServerConfigs] = None

	private def getDist = configs.getOrElse(ServerConfigs()).www
	private def getReloadUrl = configs.getOrElse(ServerConfigs()).reloadUrl

	def allRoutes: Seq[AppController] = Seq(new AppController(getDist, getReloadUrl))

	def setLogger(customLogger: CustomLogger): Unit = logger = Some(customLogger)

	def start(customLogger: CustomLogger, conf: ServerConfigs): Unit = {
		logger = Some(customLogger)
		configs = Some(conf)
		main(Array())		
	}

	def stop(): Unit = {
		logger.foreach(_.info(s"stop [${WsSession.count}] WS sessions"))
		WsSession.stop()
		server.foreach(_.stop())
	}

	override def main(args: Array[String]): Unit = {
		val port = configs.getOrElse(ServerConfigs()).port.getOrElse(10101)
		val srv = Undertow.builder
			.addHttpListener(port, "0.0.0.0")
			// increase io thread count as per https://github.com/TechEmpower/FrameworkBenchmarks/pull/4008
			.setIoThreads(Runtime.getRuntime.availableProcessors() * 2)
			// In HTTP/1.1, connections are persistent unless declared otherwise.
			// Adding a "Connection: keep-alive" header to every response would only
			// add useless bytes.
			.setServerOption[java.lang.Boolean](UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
			.setHandler(defaultHandler)
			.build
		srv.start()
		server = Some(srv)
		logger.foreach(_.info(s"[Reload for ScalaJS] Start server on http://localhost:${port}"))
	}

	def notify(customLogger: CustomLogger): Unit = {
		WsSession.notify(customLogger)
	}
}
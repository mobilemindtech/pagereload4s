package br.com.mobilemind.r4sjs.plugin

import sbt.Logger
import sbt.util.Logger

import java.time.LocalDateTime

object CustomLogger:
  def apply(logger: sbt.util.Logger) = new CustomLogger(Some(logger))

class CustomLogger(logger: Option[sbt.util.Logger]):

  import LogLevel.*

  private enum LogLevel(val level: String):
    case LevelInfo() extends LogLevel("info")
    case LevelError() extends LogLevel("error")
    case LevelDebug() extends LogLevel("error")

  def debug(s: String): Unit = log(LevelDebug(), s)

  def info(s: String): Unit = log(LevelInfo(), s)

  def error(s: String): Unit = log(LevelError(), s)

  private def log(level: LogLevel, text: String): Unit = 
    logger.foreach(_.info(s"[${level.level}][LiveReload4ScalaJS]: $text"))

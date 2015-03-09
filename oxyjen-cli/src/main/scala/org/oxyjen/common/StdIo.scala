package org.oxyjen.common

import org.slf4j.LoggerFactory

import scala.io.StdIn

object StdIo {
  private val errLog = LoggerFactory.getLogger("org.oxyjen.ozone.Main")

  def puts(str: String): Unit = {
    println(str)
  }

  def pute(str: String, args: AnyRef*): Unit = {
    errLog.warn(str, args:_*)
  }

  def readLine(prompt: String): String = {
    StdIn.readLine(prompt)
  }

  def readPassword(prompt: String): String = {
    val console = System.console()
    val password = console.readPassword(prompt)
    new String(password)
  }
}

package org.oxyjen.ozone.commands.common

import java.io.{File, IOException}

import org.apache.commons.io.FileUtils

import scala.io.Source

object TokenPersister {
  private val tokenFileName = ".oxyjen-session"

  def load(): Option[String] = {
    val tokenFile = theTokenFile()
    try {
      val source = Source.fromFile(tokenFile)
      Some(source.mkString.trim)
    } catch {
      case e: IOException =>
        None
    }
  }

  def save(token: String): Unit = {
    FileUtils.writeStringToFile(theTokenFile(), token.trim)
  }

  private def theTokenFile(): File = {
    val userHome = System.getProperty("user.home")
    new File(userHome, tokenFileName)
  }
}

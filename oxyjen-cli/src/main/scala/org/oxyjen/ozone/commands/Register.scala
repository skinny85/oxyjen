package org.oxyjen.ozone.commands

import java.util

import org.oxyjen.ozone._
import org.oxyjen.ozone.commands.common._
import org.slf4j.LoggerFactory

import scala.io.StdIn

object Register {
  private val errLog = LoggerFactory.getLogger("org.oxyjen.ozone.Main")

  def main(args: String*): Int = {
    val orgId = {
      if (args.length > 0)
        args(0)
      else
        StdIn.readLine("Organization ID to register: ")
    }

    val password = {
      if (args.length > 1)
        args(1)
      else {
        val console = System.console()
        val password = console.readPassword("Password: ")
        val password2 = console.readPassword("Repeat password: ")
        if (!util.Arrays.equals(password, password2)) {
          errLog warn "Given passwords do not match! Exiting"
          return 3
        }
        new String(password)
      }
    }

    OZoneOperations.register(orgId, password) match {
      case ConnectionError(e) =>
        CommandsUtils.connectionError(e)
      case UnexpectedError(msg) =>
        CommandsUtils.unexpectedError(msg)
      case InvalidArguments(violations) =>
        CommandsUtils.invalidArguments(violations)
      case OrgRegistered(tksid) =>
        TokenPersister.save(tksid)
        println("Organization registered")
        0
    }
  }
}

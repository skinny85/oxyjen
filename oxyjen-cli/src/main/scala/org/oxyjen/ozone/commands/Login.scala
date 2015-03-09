package org.oxyjen.ozone.commands

import org.oxyjen.ozone.commands.common._
import org.slf4j.LoggerFactory

import scala.io.StdIn

object Login {
  private val errLog = LoggerFactory.getLogger("org.oxyjen.ozone.Main")

  def main(args: String*): Int = {
    val orgId = {
      if (args.length > 0)
        args(0)
      else
        StdIn.readLine("Organization ID to login as: ")
    }

    val password = {
      if (args.length > 1)
        args(1)
      else {
        val console = System.console()
        val password = console.readPassword("Password: ")
        new String(password)
      }
    }

    OZoneCommonResponses.handleOZoneResponse(OZoneOperations.login(orgId, password)) {
      case InvalidCredentials =>
        errLog warn "Invalid Organization ID and/or password given"
        7
      case LoginSuccessful(tksid) =>
        TokenPersister.save(tksid)
        println("Login successful")
        0
    }
  }
}
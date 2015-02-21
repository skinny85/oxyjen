package org.oxyjen.ozone.commands

import java.util

import org.oxyjen.ozone._
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
        errLog warn s"There was an error connecting to the Oxyjen server (${e.getMessage}). "
        errLog warn "Please check your Internet connection and try again in a few moments. "
        4
      case UnexpectedError(msg) =>
        errLog error s"There was an unexpected error processing your request ($msg)"
        errLog error "Please verify you have the latest version of the Oxyjen " +
          "client on the Oxyjen home page http://oxyjen.org"
        5
      case InvalidArguments(violations) =>
        errLog warn "The values supplied were incorrect:"
        for (violation <- violations)
          errLog warn "\t" + violation
        6
      case OrgRegistered(_) =>
        println("Organization created")
        0
    }
  }
}

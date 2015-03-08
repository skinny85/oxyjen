package org.oxyjen.ozone.commands.common

import org.slf4j.LoggerFactory

object CommandsUtils {
  private val errLog = LoggerFactory.getLogger("org.oxyjen.ozone.Main")

  def connectionError(e: Throwable): Int = {
    errLog warn s"There was an error connecting to the Oxyjen server (${e.getMessage}). "
    errLog warn "Please check your Internet connection and try again in a few moments. "
    4
  }

  def unexpectedError(msg: String): Int = {
    errLog error s"There was an unexpected error processing your request ($msg)"
    errLog error "Please verify you have the latest version of the Oxyjen " +
      "client on the Oxyjen home page http://oxyjen.org"
    5
  }

  def unexpectedServerError(msg: String): Int = {
    errLog error s"There was an unexpected error response from the server ($msg)"
    errLog error "Please verify you have the latest version of the Oxyjen client and try again in a moment"
    5
  }

  def invalidArguments(violations: List[String]): Int = {
    errLog warn "The values supplied were incorrect:"
    for (violation <- violations)
      errLog warn "\t" + violation
    6
  }
}

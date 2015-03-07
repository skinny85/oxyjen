package org.oxyjen.ozone.commands

import org.slf4j.LoggerFactory

object Push {
  private val errLog = LoggerFactory.getLogger("org.oxyjen.ozone.Main")

  def main(args: String*): Int = {
    if (args.length != 3) {
      errLog warn "Usage: ozone push <name> <version> <file>"
      return 1
    }

    val maybeToken = TokenPersister.load()
    if (maybeToken.isEmpty) {
      errLog warn "You need to be logged in to upload a template. Execute 'ozone login' and then try again"
      return 1
    }

    val token = maybeToken.get
    val name = args(0)
    val version = args(1)
    val filePath = args(2)

    OZoneOperations.upload(token, name, version, filePath) match {
      case ConnectionError(e) =>
        CommandsUtils.connectionError(e)
      case UnexpectedError(msg) =>
        CommandsUtils.unexpectedError(msg)
      case UnexpectedServerError(msg) =>
        errLog error s"There was an unexpected error response from the server ($msg)"
        errLog error "Please verify you have the latest version of the Oxyjen client and try again in a moment"
        5
      case InvalidArguments(violations) =>
        CommandsUtils.invalidArguments(violations)
      case AuthorizationFailed =>
        errLog warn "Your session has expired. Execute 'ozone login' to sign in and try again"
        8
      case FileUploaded =>
        println("File uploaded")
        0
    }
  }
}

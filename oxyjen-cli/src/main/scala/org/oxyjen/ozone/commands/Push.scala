package org.oxyjen.ozone.commands

import org.oxyjen.ozone.commands.common._
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

    OZoneCommonResponses.handleOZoneResponse(OZoneOperations.upload(token, name, version, filePath)) {
      case FileMissing =>
        errLog.warn("The file '{}' does not exist!", filePath)
        1
      case AuthorizationFailed =>
        errLog warn "Your session has expired. Execute 'ozone login' to sign in and try again"
        8
      case FileUploaded =>
        println("File uploaded")
        0
    }
  }
}

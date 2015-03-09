package org.oxyjen.ozone.commands

import org.oxyjen.common.{ReturnCode, StdIo}
import org.oxyjen.ozone.commands.common._

object Push {
  def main(args: String*): ReturnCode = {
    if (args.length != 3) {
      StdIo pute "Usage: ozone push <name> <version> <file>"
      return ReturnCode.IncorrectNumberOfArguments
    }

    val maybeToken = TokenPersister.load()
    if (maybeToken.isEmpty) {
      StdIo pute "You need to be logged in to upload a template. Execute 'ozone login' and then try again"
      return ReturnCode.SessionRequired
    }

    val token = maybeToken.get
    val name = args(0)
    val version = args(1)
    val filePath = args(2)

    OZoneCommonResponses.handleOZoneResponse(OZoneOperations.upload(token, name, version, filePath)) {
      case FileMissing =>
        StdIo.pute("The file '{}' does not exist!", filePath)
        ReturnCode.ContradictoryArguments
      case AuthorizationFailed =>
        StdIo pute "Your session has expired. Execute 'ozone login' to sign in and try again"
        ReturnCode.SessionRequired
      case FileUploaded =>
        StdIo puts "File uploaded"
        ReturnCode.Success
    }
  }
}

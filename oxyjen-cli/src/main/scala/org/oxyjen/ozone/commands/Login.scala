package org.oxyjen.ozone.commands

import org.oxyjen.common.StdIo
import org.oxyjen.ozone.commands.common._

object Login {
  def main(args: String*): Int = {
    val orgId =
      if (args.length > 0)
        args(0)
      else
        StdIo readLine "Organization ID to login as: "

    val password =
      if (args.length > 1)
        args(1)
      else
        StdIo readPassword "Password: "

    OZoneCommonResponses.handleOZoneResponse(OZoneOperations.login(orgId, password)) {
      case InvalidCredentials =>
        StdIo pute "Invalid Organization ID and/or password given"
        7
      case LoginSuccessful(tksid) =>
        TokenPersister.save(tksid)
        StdIo puts "Login successful"
        0
    }
  }
}

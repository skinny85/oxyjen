package org.oxyjen.ozone.commands

import org.oxyjen.common.StdIo
import org.oxyjen.ozone.commands.common._

object Register {
  def main(args: String*): Int = {
    val orgId = {
      if (args.length > 0)
        args(0)
      else
        StdIo readLine "Organization ID to register: "
    }

    val password = {
      if (args.length > 1)
        args(1)
      else {
        val password = StdIo readPassword "Password: "
        val password2 = StdIo readPassword "Repeat password: "
        if (password != password2) {
          StdIo pute "Given passwords do not match! Exiting"
          return 3
        }
        password
      }
    }

    OZoneCommonResponses.handleOZoneResponse(OZoneOperations.register(orgId, password)) {
      case OrgRegistered(tksid) =>
        TokenPersister.save(tksid)
        StdIo puts "Organization registered"
        0
    }
  }
}

package org.oxyjen.ozone.commands

import org.oxyjen.common.{StdIo, ReturnCode}

object Version {
  def main(args: String*): ReturnCode = {
    StdIo puts "OxyjenZone Client version 0.2"
    ReturnCode.Success
  }
}

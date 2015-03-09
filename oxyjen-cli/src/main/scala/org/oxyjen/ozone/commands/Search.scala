package org.oxyjen.ozone.commands

import org.oxyjen.common.{ReturnCode, StdIo}
import org.oxyjen.ozone.commands.common._

object Search {
  def main(args: String*): ReturnCode = {
    if (args.length != 1) {
      StdIo pute "Usage: ozone search <term>"
      return ReturnCode.IncorrectNumberOfArguments
    }

    val term = args(0)

    OZoneCommonResponses.handleOZoneResponse(OZoneOperations.search(term)) {
      case SearchResults(results) =>
        if (results.isEmpty)
          StdIo puts "Sorry, your query did not match any results"
        else
          StdIo.puts("Found results:\n" + results.map(showSearchGrouping).mkString("\n"))
        ReturnCode.Success
    }
  }

  private def showSearchGrouping(sg: SearchGrouping): String = {
    val orgPart = if (sg.organization == "oxyjen") "" else sg.organization
    val namePart = s":${sg.name}"
    val versionPart = "\t\t\t" +
      (if (sg.versions.size == 1)
        sg.versions(0)
      else sg.versions.size + " versions (" + sg.versions.mkString(", ") + ")")
    orgPart + namePart + versionPart
  }
}

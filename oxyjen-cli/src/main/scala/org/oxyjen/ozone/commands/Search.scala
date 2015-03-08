package org.oxyjen.ozone.commands

import org.oxyjen.ozone.commands.common._
import org.slf4j.LoggerFactory

object Search {
  private val errLog = LoggerFactory.getLogger("org.oxyjen.ozone.Main")

  def main(args: String*): Int = {
    if (args.length != 1) {
      errLog warn "Usage: ozone search <term>"
      return 1
    }

    val term = args(0)

    OZoneOperations.search(term) match {
      case ConnectionError(e) =>
        CommandsUtils.connectionError(e)
      case UnexpectedError(msg) =>
        CommandsUtils.unexpectedError(msg)
      case UnexpectedServerError(msg) =>
        CommandsUtils.unexpectedError(msg)
      case SearchResults(results) =>
        if (results.isEmpty)
          println("Sorry, your query did not match any results")
        else
          println("Found results:\n" + results.map(showSearchGrouping).mkString("\n"))
        0
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

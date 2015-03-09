package org.oxyjen.ozone.commands.common

import org.oxyjen.common.StdIo

object OZoneCommonResponses {
  def handleOZoneResponse(resp: OZoneResponse)(fun: PartialFunction[OZoneResponse, Int]): Int = {
    if (fun.isDefinedAt(resp))
      fun(resp)
    else {
      resp match {
        case ConnectionError(e) =>
          StdIo pute s"There was an error connecting to the Oxyjen server (${e.getMessage}). "
          StdIo pute "Please check your Internet connection and try again in a few moments. "
          4
        case UnexpectedError(msg) =>
          StdIo pute s"There was an unexpected error processing your request ($msg)"
          StdIo pute "Please verify you have the latest version of the Oxyjen " +
            "client on the Oxyjen home page http://oxyjen.org"
          5
        case UnexpectedServerError(msg) =>
          StdIo pute s"There was an unexpected error response from the server ($msg)"
          StdIo pute "Please verify you have the latest version of the Oxyjen client and try again in a moment"
          5
        case InvalidArguments(violations) =>
          StdIo pute "The values supplied were incorrect:"
          for (violation <- violations)
            StdIo.pute("\t{}", violation)
          6
      }
    }
  }
}

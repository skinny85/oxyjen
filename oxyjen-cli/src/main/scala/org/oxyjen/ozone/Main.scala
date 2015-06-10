package org.oxyjen.ozone

import org.oxyjen.ozone.commands._
import org.oxyjen.common.{ReturnCode, StdIo}

object Main {
  def main(args: Array[String]): Unit = {
    val exitStatus = codeMain(args: _*)

    // always exit the JVM so that we don't leave any Dispatch executors hanging
//    if (exitStatus.code != 0)
      System.exit(exitStatus.code)
  }

  val USAGE =
    """Usage: ozone <command> [<args>...]
      |
      |List of commands:
      |
      |  version         Output version information and exit
      |  search          Search OxyjenZone for templates
      |  register        Register a new Organization on OxyjenZone
      |  login           Log in as your Organization to OxyjenZone
      |  push            Upload a template file to OxyjenZone""".stripMargin

  def codeMain(args: String*): ReturnCode = {
    if (args.isEmpty) {
      StdIo pute USAGE
      return ReturnCode.IncorrectNumberOfArguments
    }

    val command = args(0)
    val commandArguments = args.slice(1, args.length)
    command match {
      case "version" =>
        Version.main(commandArguments:_*)
      case "register" =>
        Register.main(commandArguments:_*)
      case "login" =>
        Login.main(commandArguments:_*)
      case "push" =>
        Push.main(commandArguments:_*)
      case "search" =>
        Search.main(commandArguments:_*)
      case "test" =>
        Test.main(commandArguments: _*)
      case _ =>
        StdIo.pute("Unrecognized command '{}'", command)
        ReturnCode.ContradictoryArguments
    }
  }
}

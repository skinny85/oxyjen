package org.oxyjen.ozone

import org.oxyjen.ozone.commands.{Search, Login, Push, Register}
import org.oxyjen.common.StdIo

object Main {
  def main(args: Array[String]): Unit = {
    val exitStatus = intMain(args: _*)

    // always exit the JVM so that we don't leave any Dispatch executors hanging
//    if (exitStatus != 0)
      System.exit(exitStatus)
  }

  val USAGE =
    """Usage: ozone <command> [<args>...]
      |
      |List of commands:
      |
      |  search          Search OxyjenZone for templates
      |  register        Register a new Organization on OxyjenZone
      |  login           Log in as your Organization to OxyjenZone
      |  push            Upload a template file to OxyjenZone""".stripMargin

  def intMain(args: String*): Int = {
    if (args.isEmpty) {
      StdIo pute USAGE
      return 1
    }

    val command = args(0)
    val commandArguments = args.slice(1, args.length)
    command match {
      case "register" =>
        Register.main(commandArguments:_*)
      case "login" =>
        Login.main(commandArguments:_*)
      case "push" =>
        Push.main(commandArguments:_*)
      case "search" =>
        Search.main(commandArguments:_*)
      case _ =>
        StdIo.pute("Unrecognized command '{}'", command)
        2
    }
  }
}

package org.oxyjen.ozone

import org.oxyjen.ozone.rest.OZoneRestClient

object DispatchOzoneMain {
  def main(args: Array[String]): Unit = {
    val exitStatus = intMain(args: _*)

    // always exit the JVM so that we don't leave any Dispatch executors hanging
//    if (exitStatus != 0)
      System.exit(exitStatus)
  }

  def intMain(args: String*): Int = {
    val caseClass = OZoneRestClient.register()
    println("result:\n" + caseClass)
    0
  }
}

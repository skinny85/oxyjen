package org.oxyjen.test

import java.io.StringReader

object StdIoTest {
  def mockConsoleToReadLines[T](line: String*)(action: => T): T = {
    Console.withIn(new StringReader(line.mkString("\n")))(action)
  }
}

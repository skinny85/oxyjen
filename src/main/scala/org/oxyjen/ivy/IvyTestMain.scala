package org.oxyjen.ivy

object IvyTestMain {
  def main(args: Array[String]): Unit = {
    println("File from Ivy: " + IvyResolver.resolve("org.apache", "log4j2", "2.1"))
    println("File from Ivy: " + IvyResolver.resolve("log4j", "log4j", "1.2.16"))
  }
}

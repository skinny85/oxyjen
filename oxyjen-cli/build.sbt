import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := "oxyjen"

version := "0.1"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "commons-io" % "commons-io" % "2.4",
  "org.apache.ivy" % "ivy" % "2.3.0",
  "net.lingala.zip4j" % "zip4j" % "1.3.2",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test")

packageArchetype.java_application

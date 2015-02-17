lazy val root = (project in file(".")).enablePlugins(UniversalPlugin)

name := "oxyjen"

version := "0.1"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "commons-io" % "commons-io" % "2.4",
  "org.apache.ivy" % "ivy" % "2.3.0",
  "net.lingala.zip4j" % "zip4j" % "1.3.2",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test")

mappings in Universal <+= (packageBin in Compile) map { jar =>
  jar -> "oxyjen.jar"
}

mappings in Universal <++= (fullClasspath in Compile) map { jars =>
  for (jar <- jars) yield
    jar.data -> ("lib/" + jar.data.getName)
}

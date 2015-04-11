lazy val `oxyjen-web` = (project in file(".")).enablePlugins(PlayScala)

name := "oxyjen-web"

version := "0.2"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "commons-io" % "commons-io" % "2.4",
  "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41"
)

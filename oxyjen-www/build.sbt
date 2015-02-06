name := "oxyjen-www"

version := "0.1"

lazy val root = Project(id ="oxyjen-www", base = file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "commons-io" % "commons-io" % "2.4",
  "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
)

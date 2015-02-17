package org.oxyjen.ozone

import dispatch._, Defaults._

case class Location(city: String, state: String)

object DispatchMain {
  def main(args: Array[String]): Unit = {
    val nyc = Location("New York", "NY")
    val future = Http(weatherSvc(nyc) OK as.String)
    for (str <- future)
      println(str)
  }

  def weatherSvc(loc: Location) = {
    host("api.wunderground.com") / "api" / "5a7c66db0ba0323a" /
      "conditions" / "q" / loc.state / (loc.city + ".xml")
  }
}

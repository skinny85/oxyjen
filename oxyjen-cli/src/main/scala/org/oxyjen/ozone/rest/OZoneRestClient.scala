package org.oxyjen.ozone.rest

import dispatch.Defaults._
import dispatch._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.Await
import scala.concurrent.duration._

object OZoneRestClient {
  def register(): OZoneResponseJson = {
    val baseReq: Req = host("localhost", 9000) / "ozone" / "api" / "register"
    val req = baseReq.setContentType("application/json", "utf-8").
      <<( """{"orgId": "a", "password": "b"}""")
    val future = Http(req.>(as.String))
    val result = Await.result(future, 60 seconds)
    val json = parse(result)
    val caseClass = json.extract[InvalidOrgJson]
    caseClass
  }

  private implicit val formats = DefaultFormats
}

package org.oxyjen.ozone.rest

import com.ning.http.client.Response
import dispatch.Defaults._
import dispatch._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object OZoneRestClient {
  def register(orgId: String, password: String): Either[Throwable, RegisterResponseJson] = {
    val req = requestFor("register").POST.setBody(requestOrgJson(orgId, password))
    val future = Http(req > RegisterDispatchHandler).either
    Await.result(future, Duration.Inf)
  }

  private def requestFor(function: String): Req = {
    (host("localhost", 9000) / "ozone" / "api" / function) setContentType(
      "application/json", "utf-8")
  }

  private def requestOrgJson(orgId: String, password: String): String = {
    s"""{"orgId": "$orgId", "password": "$password"}"""
  }
}

//case class OrgRequestJson(orgId: String, password: String)

object RegisterDispatchHandler extends (Response => RegisterResponseJson) {
  override def apply(resp: Response): RegisterResponseJson = {
    val stringResponse = resp.getResponseBody
    val json = parse(stringResponse)
    resp.getStatusCode match {
      case 400 =>
        json.extract[GenericResponseJson]
      case 422 =>
        json.extract[InvalidOrgJson]
      case 201 =>
        json.extract[OrgCreatedJson]
    }
  }

  private implicit val formats = DefaultFormats
}

sealed trait RegisterResponseJson
final case class GenericResponseJson(status: String, message: String)
  extends RegisterResponseJson
final case class InvalidOrgJson(status: String, message: String,
                                violations: List[String])
  extends RegisterResponseJson
final case class OrgCreatedJson(status: String, message: String, tksid: String)
  extends RegisterResponseJson

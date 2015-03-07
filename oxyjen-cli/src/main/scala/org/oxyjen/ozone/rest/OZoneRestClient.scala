package org.oxyjen.ozone.rest

import java.io.File

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

  def upload(name: String, version: String, filePath: String): Either[Throwable, UploadResponseJson] = {
    val req = requestFor("upload")
      .setQueryParameters(Map(
        "name" -> Seq(name),
        "version" -> Seq(version)))
      .<<<(new File(filePath))
    val future = Http(req > UploadDispatchHandler).either
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
        json.extract[ClientErrorJson]
      case 422 =>
        json.extract[InvalidArgumentsJson]
      case 201 =>
        json.extract[OrgCreatedJson]
    }
  }

  private implicit val formats = DefaultFormats
}

object UploadDispatchHandler extends (Response => UploadResponseJson) {
  override def apply(resp: Response): UploadResponseJson = {
    val stringResponse = resp.getResponseBody
    val json = parse(stringResponse)
    resp.getStatusCode match {
      case 400 =>
        json.extract[ClientErrorJson]
      case 422 =>
        json.extract[InvalidArgumentsJson]
      case 500 =>
        json.extract[ServerErrorJson]
      case 200 =>
        json.extract[FileUploadedJson]
    }
  }

  private implicit val formats = DefaultFormats
}

sealed trait RegisterResponseJson

sealed trait UploadResponseJson

final case class ClientErrorJson(status: String, message: String)
  extends RegisterResponseJson
  with UploadResponseJson
final case class InvalidArgumentsJson(status: String, message: String,
                                violations: List[String])
  extends RegisterResponseJson
  with UploadResponseJson

final case class OrgCreatedJson(status: String, message: String, tksid: String)
  extends RegisterResponseJson

final case class ServerErrorJson(status: String, message: String)
  extends UploadResponseJson
final case class FileUploadedJson(status: String, message: String)
  extends UploadResponseJson

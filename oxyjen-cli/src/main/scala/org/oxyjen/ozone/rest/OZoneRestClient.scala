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

  def login(orgId: String, password: String): Either[Throwable, LoginResponseJson] = {
    val req = requestFor("login").POST.setBody(requestOrgJson(orgId, password))
    val future = Http(req > LoginDispatchHandler).either
    Await.result(future, Duration.Inf)
  }

  def upload(token: String, name: String, version: String, filePath: String): Either[Throwable, UploadResponseJson] = {
    val req = requestFor("upload")
      .setQueryParameters(Map(
        "tksid" -> Seq(token),
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

object LoginDispatchHandler extends (Response => LoginResponseJson) {
  override def apply(resp: Response): LoginResponseJson = {
    val stringResponse = resp.getResponseBody
    val json = parse(stringResponse)
    resp.getStatusCode match {
      case 400 =>
        json.extract[ClientErrorJson]
      case 401 =>
        json.extract[InvalidCredentialsJson]
      case 200 =>
        json.extract[LoginSuccessfulJson]
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
      case 500 =>
        json.extract[ServerErrorJson]
      case 422 =>
        json.extract[InvalidArgumentsJson]
      case 401 =>
        json.extract[UnauthorizedJson]
      case 200 =>
        json.extract[FileUploadedJson]
    }
  }

  private implicit val formats = DefaultFormats
}

sealed trait RegisterResponseJson

sealed trait LoginResponseJson

sealed trait UploadResponseJson

final case class ClientErrorJson(status: String, message: String)
  extends RegisterResponseJson
  with LoginResponseJson
  with UploadResponseJson
final case class InvalidArgumentsJson(status: String, message: String,
                                violations: List[String])
  extends RegisterResponseJson
  with UploadResponseJson

final case class OrgCreatedJson(status: String, message: String, tksid: String)
  extends RegisterResponseJson

final case class InvalidCredentialsJson(status: String, message: String)
  extends LoginResponseJson
final case class LoginSuccessfulJson(status: String, message: String, tksid: String)
  extends LoginResponseJson

final case class ServerErrorJson(status: String, message: String)
  extends UploadResponseJson
final case class UnauthorizedJson(status: String, message: String)
  extends UploadResponseJson
final case class FileUploadedJson(status: String, message: String)
  extends UploadResponseJson

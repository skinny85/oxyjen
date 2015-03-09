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
    performRequest(req, RegisterDispatchHandler)
  }

  def login(orgId: String, password: String): Either[Throwable, LoginResponseJson] = {
    val req = requestFor("login").POST.setBody(requestOrgJson(orgId, password))
    performRequest(req, LoginDispatchHandler)
  }

  def upload(token: String, name: String, version: String, file: File): Either[Throwable, UploadResponseJson] = {
    val req = requestFor("upload")
      .setQueryParameters(Map(
        "tksid" -> Seq(token),
        "name" -> Seq(name),
        "version" -> Seq(version)))
      .<<<(file)
    performRequest(req, UploadDispatchHandler)
  }

  def search(term: String): Either[Throwable, SearchResponseJson] = {
    val req = requestFor("search").setBody(s"""{"term": "$term"}""")
    performRequest(req, SearchDispatchHandler)
  }

  private def requestFor(function: String): Req = {
    (host("localhost", 9000) / "ozone" / "api" / function) setContentType(
      "application/json", "utf-8")
  }

  private def requestOrgJson(orgId: String, password: String): String = {
    s"""{"orgId": "$orgId", "password": "$password"}"""
  }

  private def performRequest[R](req: Req, handler: Response => R): Either[Throwable, R] = {
    Await.result(Http(req > handler).either, Duration.Inf)
  }
}

abstract class AbstractJsonDispatchHandler[R <: OZoneResponseJson] extends (Response => R) {
  final def apply(resp: Response): R = {
    val stringResponseBody = resp.getResponseBody
    implicit val json = parse(stringResponseBody)
    val code = resp.getStatusCode
    if (handle.isDefinedAt(code, json))
      handle(code, json)
    else code match {
      case 400 => extractJson[ClientErrorJson].asInstanceOf[R]
      case 422 => extractJson[InvalidArgumentsJson].asInstanceOf[R]
      case 500 => extractJson[ServerErrorJson].asInstanceOf[R]
    }
  }

  private def extractJson[T](implicit json: JValue, manifest: Manifest[T]): T = {
    json.extract[T]
  }

  protected def handle: PartialFunction[(Int, JValue), R]

  protected implicit val formats = DefaultFormats
}

object RegisterDispatchHandler extends AbstractJsonDispatchHandler[RegisterResponseJson] {
  protected def handle = {
    case (201, json) => json.extract[OrgCreatedJson]
  }
}

object LoginDispatchHandler extends AbstractJsonDispatchHandler[LoginResponseJson] {
  protected def handle = {
    case (200, json) => json.extract[LoginSuccessfulJson]
  }
}

object UploadDispatchHandler extends AbstractJsonDispatchHandler[UploadResponseJson] {
  protected def handle = {
    case (401, json) => json.extract[UnauthorizedJson]
    case (200, json) => json.extract[FileUploadedJson]
  }
}

object SearchDispatchHandler extends AbstractJsonDispatchHandler[SearchResponseJson] {
  protected def handle = {
    case (200, json) => json.extract[SearchResultsJson]
  }
}

sealed trait OZoneResponseJson

sealed trait RegisterResponseJson extends OZoneResponseJson

sealed trait LoginResponseJson extends OZoneResponseJson

sealed trait UploadResponseJson extends OZoneResponseJson

sealed trait SearchResponseJson extends OZoneResponseJson

final case class ClientErrorJson(status: String, message: String)
  extends RegisterResponseJson
  with LoginResponseJson
  with UploadResponseJson
  with SearchResponseJson
final case class ServerErrorJson(status: String, message: String)
  extends UploadResponseJson
  with SearchResponseJson
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

final case class UnauthorizedJson(status: String, message: String)
  extends UploadResponseJson
final case class FileUploadedJson(status: String, message: String)
  extends UploadResponseJson

final case class SearchResultsJson(status: String, results: List[SearchGroupingJson])
  extends SearchResponseJson
final case class SearchGroupingJson(organization: String, name: String,
                                    versions: List[String])

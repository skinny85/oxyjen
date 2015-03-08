package org.oxyjen.ozone.commands.common

import java.io.File

import org.oxyjen.ozone.rest._

object OZoneOperations {
  def register(orgId: String, password: String): RegisterResponse = {
    handleRestResponse[RegisterResponse](OZoneRestClient.register(orgId, password)) {
      case OrgCreatedJson(_, _, tksid) => OrgRegistered(tksid)
    }
  }

  def login(orgId: String, password: String): LoginResponse = {
    handleRestResponse(OZoneRestClient.login(orgId, password)) {
      case InvalidCredentialsJson(_, _) => InvalidCredentials
      case LoginSuccessfulJson(_, _, tksid) => LoginSuccessful(tksid)
    }
  }

  def upload(token: String, name: String, version: String, filePath: String): UploadResponse = {
    val file = new File(filePath)
    // TODO think about moving this check into OZoneRestClient.upload
    if (!file.exists())
      return FileMissing

    handleRestResponse(OZoneRestClient.upload(token, name, version, file)) {
      case UnauthorizedJson(_, _) => AuthorizationFailed
      case FileUploadedJson(_, _) => FileUploaded
    }
  }

  def search(term: String): SearchResponse = {
    handleRestResponse(OZoneRestClient.search(term)) {
      case SearchResultsJson(_, results) =>
        SearchResults(results.map(s =>
          SearchGrouping(s.organization, s.name, s.versions)))
    }
  }

  private def handleRestResponse[R <: OZoneResponse]
      (maybeResponse: Either[Throwable, OZoneResponseJson])
      (fun: PartialFunction[OZoneResponseJson, R]): R = {
    maybeResponse match {
      case Left(e) => ConnectionError(e).asInstanceOf[R]
      case Right(resp) =>
        if (fun.isDefinedAt(resp))
          fun(resp)
        else {
          resp match {
            case ClientErrorJson(_, msg) =>
              UnexpectedError(msg).asInstanceOf[R]
            case ServerErrorJson(_, msg) =>
              UnexpectedServerError(msg).asInstanceOf[R]
            case InvalidArgumentsJson(_, _, violations) =>
              InvalidArguments(violations).asInstanceOf[R]
          }
        }
    }
  }
}

sealed trait OZoneResponse

sealed trait RegisterResponse extends OZoneResponse {
  def successful: Boolean
}
protected[ozone] sealed trait SuccessfulRegisterResponse extends RegisterResponse {
  def successful = true
}
protected[ozone] sealed trait UnsuccessfulRegisterResponse extends RegisterResponse {
  def successful = false
}

sealed trait LoginResponse extends OZoneResponse

sealed trait UploadResponse extends OZoneResponse

sealed trait SearchResponse extends OZoneResponse

case class ConnectionError(e: Throwable)
  extends UnsuccessfulRegisterResponse
  with LoginResponse
  with UploadResponse
  with SearchResponse
case class UnexpectedError(message: String)
  extends UnsuccessfulRegisterResponse
  with LoginResponse
  with UploadResponse
  with SearchResponse
case class UnexpectedServerError(message: String)
  extends UploadResponse
  with SearchResponse
case class InvalidArguments(violations: List[String])
  extends UnsuccessfulRegisterResponse
  with UploadResponse
case class OrgRegistered(tksid: String) extends SuccessfulRegisterResponse

case object InvalidCredentials extends LoginResponse
case class LoginSuccessful(tksid: String) extends LoginResponse

case object FileMissing extends UploadResponse
case object AuthorizationFailed extends UploadResponse
case object FileUploaded extends UploadResponse

case class SearchResults(results: List[SearchGrouping]) extends SearchResponse
case class SearchGrouping(organization: String, name: String, versions: List[String])

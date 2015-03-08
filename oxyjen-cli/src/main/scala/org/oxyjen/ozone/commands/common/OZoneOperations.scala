package org.oxyjen.ozone.commands.common

import java.io.File

import org.oxyjen.ozone.rest._

object OZoneOperations {
  def register(orgId: String, password: String): RegisterResponse = {
    OZoneRestClient.register(orgId, password) match {
      case Left(e) => ConnectionError(e)
      case Right(json) => json match {
        case ClientErrorJson(_, msg) => UnexpectedError(msg)
        case InvalidArgumentsJson(_, _, violations) => InvalidArguments(violations)
        case OrgCreatedJson(_, _, tksid) => OrgRegistered(tksid)
      }
    }
  }

  def login(orgId: String, password: String): LoginResponse = {
    OZoneRestClient.login(orgId, password) match {
      case Left(e) => ConnectionError(e)
      case Right(json) => json match {
        case ClientErrorJson(_, msg) => UnexpectedError(msg)
        case InvalidCredentialsJson(_, _) => InvalidCredentials
        case LoginSuccessfulJson(_, _, tksid) => LoginSuccessful(tksid)
      }
    }
  }

  def upload(token: String, name: String, version: String, filePath: String): UploadResponse = {
    val file = new File(filePath)
    // TODO think about moving this check into OZoneRestClient.upload
    if (!file.exists())
      return FileMissing

    OZoneRestClient.upload(token, name, version, file) match {
      case Left(e) => ConnectionError(e)
      case Right(json) => json match {
        case ClientErrorJson(_, msg) => UnexpectedError(msg)
        case ServerErrorJson(_, msg) => UnexpectedServerError(msg)
        case InvalidArgumentsJson(_, _, violations) => InvalidArguments(violations)
        case UnauthorizedJson(_, _) => AuthorizationFailed
        case FileUploadedJson(_, _) => FileUploaded
      }
    }
  }

  def search(term: String): SearchResponse = {
    OZoneRestClient.search(term) match {
      case Left(e) => ConnectionError(e)
      case Right(json) => json match {
        case ClientErrorJson(_, msg) => UnexpectedError(msg)
        case ServerErrorJson(_, msg) => UnexpectedServerError(msg)
        case SearchResultsJson(_, results) =>
          SearchResults(results.map(s =>
            SearchGrouping(s.organization, s.name, s.versions)))
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

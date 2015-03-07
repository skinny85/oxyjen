package org.oxyjen.ozone.commands

import org.oxyjen.ozone.rest._

object OZoneOperations {
  def register(orgId: String, password: String): RegisterResponse = {
    OZoneRestClient.register(orgId, password) match {
      case Left(e) => ConnectionError(e)
      case Right(json) => json match {
        case ClientErrorJson(_, message) => UnexpectedError(message)
        case InvalidArgumentsJson(_, _, violations) => InvalidArguments(violations)
        case OrgCreatedJson(_, _, tksid) => OrgRegistered(tksid)
      }
    }
  }

  def upload(name: String, version: String, filePath: String): UploadResponse = {
    OZoneRestClient.upload(name, version, filePath) match {
      case Left(e) => ConnectionError(e)
      case Right(json) => json match {
        case ClientErrorJson(_, message) => UnexpectedError(message)
        case InvalidArgumentsJson(_, _, violations) => InvalidArguments(violations)
        case ServerErrorJson(_, msg) => UnexpectedServerError(msg)
        case FileUploadedJson(_, _) => FileUploaded
      }
    }
  }
}

sealed trait RegisterResponse {
  def successful: Boolean
}
protected[ozone] sealed trait SuccessfulRegisterResponse extends RegisterResponse {
  def successful = true
}
protected[ozone] sealed trait UnsuccessfulRegisterResponse extends RegisterResponse {
  def successful = false
}

sealed trait UploadResponse

case class ConnectionError(e: Throwable)
  extends UnsuccessfulRegisterResponse
  with UploadResponse
case class UnexpectedError(message: String)
  extends UnsuccessfulRegisterResponse
  with UploadResponse
case class UnexpectedServerError(message: String)
  extends UploadResponse
case class InvalidArguments(violations: List[String])
  extends UnsuccessfulRegisterResponse
  with UploadResponse
case class OrgRegistered(tksid: String) extends SuccessfulRegisterResponse

case object FileUploaded extends UploadResponse

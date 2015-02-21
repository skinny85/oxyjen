package org.oxyjen.ozone

import org.oxyjen.ozone.rest.{OrgCreatedJson, InvalidOrgJson, GenericResponseJson, OZoneRestClient}

object OZoneOperations {
  def register(orgId: String, password: String): RegisterResponse = {
    OZoneRestClient.register(orgId, password) match {
      case Left(e) => ConnectionError(e)
      case Right(json) => json match {
        case GenericResponseJson(_, message) => UnexpectedError(message)
        case InvalidOrgJson(_, _, violations) => InvalidArguments(violations)
        case OrgCreatedJson(_, _, tksid) => OrgRegistered(tksid)
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
case class ConnectionError(e: Throwable) extends UnsuccessfulRegisterResponse
case class UnexpectedError(message: String) extends UnsuccessfulRegisterResponse
case class InvalidArguments(violations: List[String]) extends UnsuccessfulRegisterResponse
case class OrgRegistered(tksid: String) extends SuccessfulRegisterResponse

package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._

object JsonApiCtrl extends Controller {
  case class OrganizationJson(orgId: String, password: String)

  implicit val organizationJsonReads: Reads[OrganizationJson] = (
    (JsPath \ "orgId").read[String] and
    (JsPath \ "password").read[String]
  )(OrganizationJson.apply _)

  def register = Action(BodyParsers.parse.json) { implicit request =>
    val organizationJsonBind = request.body.validate[OrganizationJson]
    organizationJsonBind.fold(
      errors => {
        BadRequest(Json.obj("status" -> "ERROR", "message" -> JsError.toFlatJson(errors)))
      },
      organizationJson => {
        OrganizationRepository.create(organizationJson.orgId, organizationJson.password) match {
          case Left(violations) =>
            UnprocessableEntity(Json.obj("status" -> "ERROR", "message" -> "Invalid arguments",
              "violations" -> violations.map(_.message)))
          case Right(tksid) =>
            Created(Json.obj("status" -> "OK", "message" -> "Organization created",
              "tksid" -> tksid))
        }
      }
    )
  }

  def login = Action(BodyParsers.parse.json) { implicit request =>
    val organizationJsonBind = request.body.validate[OrganizationJson]
    organizationJsonBind.fold(
      errors => {
        BadRequest(Json.obj("status" -> "ERROR", "message" -> JsError.toFlatJson(errors)))
      },
      organizationJson => {
        OzoneSecurity.login(organizationJson.orgId, organizationJson.password) match {
          case None =>
            Unauthorized(Json.obj("status" -> "ERROR", "message" -> "Invalid credentials supplied"))
          case Some(tksid) =>
            Ok(Json.obj("status" -> "OK", "message" -> "Authentication successful", "tksid" -> tksid))
        }
      }
    )
  }
}

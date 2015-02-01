package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._

object JsonApiCtrl extends Controller {
  case class RegisterJson(orgId: String, password: String)

  implicit val registerJsonReads: Reads[RegisterJson] = (
    (JsPath \ "orgId").read[String] and
    (JsPath \ "password").read[String]
  )(RegisterJson.apply _)

  def register = Action(BodyParsers.parse.json) { implicit request =>
    val registerJsonBind = request.body.validate[RegisterJson]
    registerJsonBind.fold(
      errors => {
        BadRequest(Json.obj("status" -> "ERROR", "message" -> JsError.toFlatJson(errors)))
      },
      registerJson => {
        OrganizationRepository.create(registerJson.orgId, registerJson.password) match {
          case InvalidOrgArguments(violations) =>
            UnprocessableEntity(Json.obj("status" -> "ERROR", "message" -> "Invalid arguments",
              "violations" -> violations.map(_.message)))
          case SuccessfulOrgCreation(id) =>
            Created(Json.obj("status" -> "OK", "message" -> "Organization created"))
        }
      }
    )
  }
}

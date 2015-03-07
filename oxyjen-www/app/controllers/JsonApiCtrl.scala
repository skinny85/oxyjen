package controllers

import models._
import models.util.Futures
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Either.RightProjection
import scala.util.{Failure, Success}

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

  def upload = Action.async(parse.temporaryFile) { implicit request =>
    (for {
      name <- requiredRequestParam("name")
      version <- requiredRequestParam("version")
      org <- OrganizationRepository.find("dummy").toRight("Organization 'dummy' not found").right
    } yield Upload.upload(org, name, version, request.body.file)) match {
      case Left(msg) =>
        Future.successful(BadRequest(Json.obj("status" -> "ERROR", "message" -> msg)))
      case Right(future) =>
        Futures.mapTry(future) {
          case Failure(e) =>
            InternalServerError(Json.obj("status" -> "ERROR", "message" -> e.getMessage))
          case Success(either) => either match {
            case Left(violations) =>
              UnprocessableEntity(Json.obj("status" -> "ERROR",
                "message" -> "Invalid arguments",
                "violations" -> violations.map(_.message)))
            case Right(_) =>
              Ok(Json.obj("status" -> "OK", "message" -> "Archive uploaded"))
          }
        }
    }
  }

  private def requiredRequestParam[A](param: String)(implicit req: Request[A]): RightProjection[String, String] = {
    req.getQueryString(param).toRight(s"Missing required parameter '$param'").right
  }

  private implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

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
      tksid <- requiredRequestParam("tksid")
      name <- requiredRequestParam("name")
      version <- requiredRequestParam("version")
    } yield (tksid, name, version)) match {
      case Left(msg) =>
        Future.successful(BadRequest(Json.obj("status" -> "ERROR", "message" -> msg)))
      case Right((tksid, name, version)) =>
        OzoneSecurity.verifyToken(tksid) match {
          case None =>
            Future.successful(Unauthorized(Json.obj("status" -> "ERROR", "message" -> "Invalid token")))
          case Some(org) =>
            Futures.mapTry(Upload.upload(org, name, version, request.body.file)) {
              case Failure(e) =>
                InternalServerError(Json.obj("status" -> "ERROR", "message" -> e.getMessage))
              case Success(either) => either match {
                case Left(violations) =>
                  UnprocessableEntity(Json.obj(
                    "status" -> "ERROR",
                    "message" -> "Invalid arguments",
                    "violations" -> violations.map(_.message)))
                case Right(_) =>
                  Ok(Json.obj("status" -> "OK", "message" -> "Archive uploaded"))
              }
            }
        }
    }
  }

  def search = Action.async(BodyParsers.parse.json) { implicit request =>
    request.body.validate[SearchQueryJson].fold(
      errors => Future.successful(
        BadRequest(Json.obj("status" -> "ERROR", "message" -> JsError.toFlatJson(errors)))),
      searchQueryJson => {
        Futures.mapTry(Artifacts.matching(searchQueryJson.term)) {
          case Failure(e) =>
            InternalServerError(Json.obj("status" -> "ERROR", "message" -> e.getMessage))
          case Success(resultsMap) =>
            Ok(Json.obj(
              "status" -> "OK",
              "results" -> resultsMap.toSeq.map(v => Json.obj(
                "organization" -> v._1._1,
                "name" -> v._1._2,
                "versions" -> v._2)
              )
            ))
        }
      }
    )
  }

  private def requiredRequestParam[A](param: String)(implicit req: Request[A]): RightProjection[String, String] = {
    req.getQueryString(param).toRight(s"Missing required parameter '$param'").right
  }

  private case class SearchQueryJson(term: String)

  private implicit val searchQueryJsonReads = Json.reads[SearchQueryJson]
  private implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

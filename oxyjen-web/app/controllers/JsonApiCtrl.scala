package controllers

import models._
import models.util.Futures
import play.api.data.validation.ValidationError
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Either.RightProjection
import scala.util.{Failure, Success}

object JsonApiCtrl extends Controller {
  private case class OrganizationJson(orgId: String, password: String)

  def register = Action(BodyParsers.parse.json) { implicit request =>
    val organizationJsonBind = request.body.validate[OrganizationJson]
    organizationJsonBind.fold(
      errors => incorrectJsonFormat(errors),
      organizationJson => {
        OrganizationRepository.create(organizationJson.orgId, organizationJson.password) match {
          case Left(violations) =>
            constraintViolation(violations)
          case Right(tksid) =>
            Created(authenticatedJson(tksid, "Organization created"))
        }
      }
    )
  }

  def login = Action(BodyParsers.parse.json) { implicit request =>
    val organizationJsonBind = request.body.validate[OrganizationJson]
    organizationJsonBind.fold(
      errors => incorrectJsonFormat(errors),
      organizationJson => {
        OzoneSecurity.login(organizationJson.orgId, organizationJson.password) match {
          case None =>
            unauthorized("Invalid credentials supplied")
          case Some(tksid) =>
            Ok(authenticatedJson(tksid, "Authentication successful"))
        }
      }
    )
  }

  private def authenticatedJson(tksid: String, msg: String): JsObject = {
    Json.obj("status" -> "OK", "message" -> msg, "tksid" -> tksid)
  }

  def upload = Action.async(parse.temporaryFile) { implicit request =>
    (for {
      tksid <- requiredRequestParam("tksid")
      name <- requiredRequestParam("name")
      version <- requiredRequestParam("version")
    } yield (tksid, name, version)) match {
      case Left(msg) =>
        Future.successful(badRequest(msg))
      case Right((tksid, name, version)) =>
        OzoneSecurity.verifyToken(tksid) match {
          case None =>
            Future.successful(unauthorized("Invalid token"))
          case Some(org) =>
            Futures.mapTry(Upload.upload(org, name, version, request.body.file)) {
              case Failure(e) =>
                unknownError(e)
              case Success(either) => either match {
                case Left(violations) =>
                  constraintViolation(violations)
                case Right(_) =>
                  Ok(Json.obj("status" -> "OK", "message" -> "Archive uploaded"))
              }
            }
        }
    }
  }

  private def requiredRequestParam[A](param: String)(implicit req: Request[A]): RightProjection[String, String] = {
    req.getQueryString(param).toRight(s"Missing required parameter '$param'").right
  }

  private case class SearchQueryJson(term: String)

  def search = Action.async(BodyParsers.parse.json) { implicit request =>
    request.body.validate[SearchQueryJson].fold(
      errors => Future.successful(incorrectJsonFormat(errors)),
      searchQueryJson => {
        Futures.mapTry(Artifacts.matching(searchQueryJson.term)) {
          case Failure(e) =>
            unknownError(e)
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

  private def incorrectJsonFormat(errors: Seq[(JsPath, Seq[ValidationError])]) = {
    badRequest(JsError.toFlatJson(errors))
  }

  private def badRequest(msg: JsValueWrapper) = {
    BadRequest(Json.obj("status" -> "ERROR", "message" -> msg))
  }

  private def constraintViolation(violations: ConstraintViolations) = {
    UnprocessableEntity(Json.obj("status" -> "ERROR", "message" -> "Invalid arguments",
      "violations" -> violations.map(_.message)))
  }

  private def unauthorized(msg: String) = {
    Unauthorized(Json.obj("status" -> "ERROR", "message" -> msg))
  }

  private def unknownError(e: Throwable) = {
    InternalServerError(Json.obj("status" -> "ERROR", "message" -> e.getMessage))
  }

  private implicit val organizationJsonReads = Json.reads[OrganizationJson]
  private implicit val searchQueryJsonReads = Json.reads[SearchQueryJson]
  private implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

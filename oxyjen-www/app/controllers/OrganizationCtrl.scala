package controllers

import models.util.Futures
import play.api.Logger
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.{CtrlFormDataUtil, CtrlSecurityUtil}

import models._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object OrganizationCtrl extends Controller {

  object MenuItemsEnum extends Enumeration {
    type MenuItem = Value
    val Main, Edit, Upload, Artifacts = Value
  }

  def main = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.SignInCtrl.login())
      case Some(org) =>
        Ok(views.html.ozone.organization.main(org))
    }
  }

  val editForm = Form(
    single(
      "desc" -> text
    )
  )

  def edit = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.SignInCtrl.login())
      case Some(org) =>
        Ok(views.html.ozone.organization.edit(org))
    }
  }

  def editPost = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.SignInCtrl.login())
      case Some(org) =>
        val boundEditForm = editForm.bindFromRequest()
        boundEditForm.fold(
          errors => {
            BadRequest(views.html.ozone.organization.edit(org))
          },
          desc => {
            OrganizationRepository.update(org.copy(desc = desc))
            Redirect(routes.OrganizationCtrl.edit()).flashing("message" -> "Organization info updated")
          }
        )
    }
  }

  case class UploadViewModel(name: String, version: String)

  val uploadForm = Form(
    mapping(
      "name" -> text,
      "version" -> text
    )(UploadViewModel.apply)(UploadViewModel.unapply)
  )

  def upload = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.SignInCtrl.login())
      case Some(org) =>
        Ok(views.html.ozone.organization.upload(org, uploadForm))
    }
  }

  def uploadPost = Action.async(parse.multipartFormData) { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Future.successful(Redirect(routes.SignInCtrl.login()))
      case Some(org) =>
        val boundUploadForm = uploadForm.bindFromRequest()
        boundUploadForm.fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.ozone.organization.upload(org, formWithErrors)))
          },
          uploadViewModel => {
            request.body.file("archive") match {
              case None =>
                Futures.mapTry(Upload.validate(org, uploadViewModel.name, uploadViewModel.version)) {
                  case Success(violations) =>
                    val formToDisplay = CtrlFormDataUtil
                      .addViolations(violations, boundUploadForm)
                      .withError("archive", "No file given")
                    Ok(views.html.ozone.organization.upload(org, formToDisplay))
                  case Failure(e) =>
                    Logger.warn("Archive validation failed", e)
                    Ok(views.html.ozone.organization.upload(org,
                      boundUploadForm.withGlobalError(s"Error processing request (${e.getMessage})")))
                }
              case Some(archive) =>
                val tmpFile = archive.ref.file

                Futures.mapTry(Upload.upload(org, uploadViewModel.name,
                    uploadViewModel.version, tmpFile)) {
                  case Success(maybeViolations) => maybeViolations match {
                    case Left(violations) =>
                      Ok(views.html.ozone.organization.upload(org,
                        CtrlFormDataUtil.addViolations(violations, boundUploadForm)))
                    case Right(_) =>
                      Redirect(routes.OrganizationCtrl.upload()).flashing("message" -> "Uploaded successfully")
                  }
                  case Failure(e) =>
                    Logger.warn("Archive upload failed!", e)
                    Ok(views.html.ozone.organization.upload(org,
                      boundUploadForm.withGlobalError(s"Upload failed (${e.getMessage})")))
                }
            }
          }
        )
    }
  }

  def artifacts = Action.async { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Future.successful(Redirect(routes.SignInCtrl.login()))
      case Some(org) =>
        Futures.mapTry(Artifacts.search(org)) {
          case Success(results) =>
            Ok(views.html.ozone.organization.artifacts(org, Right(results)))
          case Failure(t) =>
            Ok(views.html.ozone.organization.artifacts(org, Left(t)))
        }
    }
  }

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

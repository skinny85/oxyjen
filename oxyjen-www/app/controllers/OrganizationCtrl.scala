package controllers

import play.api.Logger
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.{CtrlFormDataUtil, CtrlSecurityUtil}

import models._

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Failure, Success, Try}

object OrganizationCtrl extends Controller {

  object MenuItemsEnum extends Enumeration {
    type MenuItem = Value
    val Main, Edit, Emails, Upload = Value
  }

  def main = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.MainOzoneCtrl.loginPage())
      case Some(org) =>
        Ok(views.html.ozone.organization.main(org))
    }
  }

  def edit = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.MainOzoneCtrl.loginPage())
      case Some(org) =>
        Ok(views.html.ozone.organization.edit(org))
    }
  }

  def emails = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.MainOzoneCtrl.loginPage())
      case Some(org) =>
        Ok(views.html.ozone.organization.emails(org))
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
        Redirect(routes.MainOzoneCtrl.loginPage())
      case Some(org) =>
        Ok(views.html.ozone.organization.upload(org, uploadForm))
    }
  }

  def handleUpload = Action.async(parse.multipartFormData) { implicit request =>
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Future.successful(Redirect(routes.MainOzoneCtrl.loginPage()))
      case Some(org) =>
        val boundUploadForm = uploadForm.bindFromRequest()
        boundUploadForm.fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.ozone.organization.upload(org, formWithErrors)))
          },
          uploadViewModel => {
            request.body.file("archive") match {
              case None =>
                val violations = Upload.validate(uploadViewModel.name, uploadViewModel.version)
                val formToDisplay = CtrlFormDataUtil
                  .addViolations(violations, boundUploadForm)
                  .withError("archive", "No file given")
                Future.successful(Ok(views.html.ozone.organization.upload(org, formToDisplay)))
              case Some(archive) =>
                val tmpFile = archive.ref.file

                Upload.upload(org, uploadViewModel.name, uploadViewModel.version, tmpFile) match {
                  case Left(violations) =>
                    Future.successful(Ok(views.html.ozone.organization.upload(org,
                      CtrlFormDataUtil.addViolations(violations, boundUploadForm))))
                  case Right(future) =>
                    mapTry(future) {
                      case Success(wsResponse) =>
                        Redirect(routes.OrganizationCtrl.upload()).flashing("message" -> "Upload OK")
                      case Failure(e) =>
                        Logger.warn("Archive upload failed!", e)
                        Ok(views.html.ozone.organization.upload(org,
                          boundUploadForm.withGlobalError(s"Upload failed (${e.getMessage})")))
                    }
                }
            }
          }
        )
    }
  }

  def mapTry[T, S](future: Future[T])(f: Try[T] => S)
                          (implicit context: ExecutionContext): Future[S] = {
    val promise = Promise[S]()
    future.onComplete[Unit](tryT => promise.success(f(tryT)))
    promise.future
  }
}

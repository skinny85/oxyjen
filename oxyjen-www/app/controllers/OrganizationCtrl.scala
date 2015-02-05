package controllers

import play.api.Logger
import play.api.libs.Files
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.{CtrlFormDataUtil, CtrlSecurityUtil}

import models._

import scala.io.Source

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

  def handleUpload = Action(parse.multipartFormData) { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.MainOzoneCtrl.loginPage())
      case Some(org) =>
        val boundUploadForm = uploadForm.bindFromRequest()
        boundUploadForm.fold(
          formWithErrors => {
            BadRequest(views.html.ozone.organization.upload(org, formWithErrors))
          },
          uploadViewModel => {
            request.body.file("archive") match {
              case None =>
                val violations = Upload.validate(uploadViewModel.name, uploadViewModel.version)
                val formToDisplay = CtrlFormDataUtil
                  .addViolations(violations, boundUploadForm)
                  .withError("archive", "No file given")
                Ok(views.html.ozone.organization.upload(org, formToDisplay))
              case Some(archive) =>
                val tmpFile = archive.ref.file

//                val source = Source.fromFile(tmpFile)
//                val contents = source.mkString
//                source.close()
//                Logger.info(s"file contents: $contents")

                val result = Upload.upload(org, uploadViewModel.name, uploadViewModel.version, tmpFile)

                Redirect(routes.OrganizationCtrl.upload()).flashing("message" -> "Upload OK")
            }
          }
        )
    }
  }
}

package controllers

import play.api.Logger
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.CtrlSecurityUtil

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

  def upload = Action { implicit request =>
    CtrlSecurityUtil.loggedIn() match {
      case None =>
        Redirect(routes.MainOzoneCtrl.loginPage())
      case Some(org) =>
        Ok(views.html.ozone.organization.upload(org))
    }
  }

  def handleUpload = Action(parse.multipartFormData) { implicit request =>
    request.body.file("archive") match {
      case None =>
        Redirect(routes.OrganizationCtrl.upload()).flashing("error" -> "Missing file")
      case Some(archive) =>
        val tmpFile = archive.ref.file
        val source = Source.fromFile(tmpFile)
        val contents = source.mkString
        source.close()
        Logger.info(s"file contents: $contents")
        Redirect(routes.OrganizationCtrl.upload()).flashing("message" -> "Upload OK")
    }
  }
}

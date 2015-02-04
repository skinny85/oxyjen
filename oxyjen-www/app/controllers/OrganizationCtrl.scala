package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.CtrlSecurityUtil

import models._

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
}

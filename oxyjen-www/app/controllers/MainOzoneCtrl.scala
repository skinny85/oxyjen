package controllers

import controllers.util.CtrlSecurityUtil

import play.api.mvc._

object MainOzoneCtrl extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.ozone.index(CtrlSecurityUtil.loggedIn()))
  }
}

package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.CtrlSecurityUtil

object MainOzoneCtrl extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.ozone.index(CtrlSecurityUtil.loggedIn()))
  }
}

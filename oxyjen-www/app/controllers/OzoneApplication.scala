package controllers

import play.api.mvc._

object OzoneApplication extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.ozone.index())
  }

  def login = Action { implicit request =>
    Redirect(routes.OzoneApplication.index()).flashing(("message", "You have been logged in"))
  }
}

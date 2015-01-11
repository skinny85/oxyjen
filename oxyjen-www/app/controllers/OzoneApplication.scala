package controllers

import play.api.mvc._

object OzoneApplication extends Controller {
  def index = Action {
    Ok(views.html.ozone.index())
  }
}

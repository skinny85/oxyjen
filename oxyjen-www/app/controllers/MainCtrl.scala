package controllers

import play.api.mvc._

object MainCtrl extends Controller {
  def index = Action {
    Ok(views.html.index())
  }
}

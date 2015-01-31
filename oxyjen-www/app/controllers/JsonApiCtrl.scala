package controllers

import play.api.libs.json._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._

object JsonApiCtrl extends Controller {

  def testJson = Action { implicit  request =>
    val json = Json.obj("key" -> "val")
    Ok(json)
  }
}

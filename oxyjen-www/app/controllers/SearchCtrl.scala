package controllers

import models.Artifacts

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.Future

object SearchCtrl extends Controller {
  val searchForm = Form(
    single(
      "terms" -> text
    )
  )

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def search = Action.async { implicit request =>
    val boundSearchForm = searchForm.bindFromRequest()
    boundSearchForm.fold(
      errors => {
        Future.successful(BadRequest(views.html.ozone.search_results()))
      },
      terms => {
        Artifacts.search(terms).map(results =>
          Ok(views.html.ozone.search_results(results)))
      }
    )
  }
}

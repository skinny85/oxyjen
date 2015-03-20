package controllers

import controllers.util.CtrlSecurityUtil
import models._
import models.util.Futures
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Success, Failure}

object SearchCtrl extends Controller {
  case class SearchViewModel(term: String)

  private val searchForm = Form(
    mapping(
      "term" -> nonEmptyText
    )(SearchViewModel.apply)(SearchViewModel.unapply)
  )

  def search = Action { implicit request =>
    Ok(views.html.ozone.search(searchForm, CtrlSecurityUtil.loggedIn()))
  }

  def searchPost = Action.async ( implicit request => {
    val maybeOrg = CtrlSecurityUtil.loggedIn()
    val boundForm = searchForm.bindFromRequest()
    boundForm.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.ozone.search(formWithErrors, maybeOrg)))
      },
      searchViewModel => {
        Futures.mapTry(Artifacts.matching(searchViewModel.term)) {
          case Failure(e) =>
            InternalServerError(views.html.ozone.search(searchForm.withGlobalError(
                s"The was an error performing the search (${e.getMessage})"),
              maybeOrg))
          case Success(results) =>
            Ok(views.html.ozone.search(searchForm.fill(searchViewModel), maybeOrg, Some(results)))
        }
      }
    )
  })

  private implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

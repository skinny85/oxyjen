package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._

object MainOzoneCtrl extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.ozone.index())
  }

  case class LoginViewModel(orgId: String, password: String)

  val loginForm = Form(
    mapping(
      "orgId" -> text,
      "password" -> text
    )(LoginViewModel.apply)(LoginViewModel.unapply)
  )

  def login = Action { implicit request =>
    val loginFormViewModel = loginForm.bindFromRequest().get
    OzoneSecurity.login(loginFormViewModel.orgId, loginFormViewModel.password) match {
      case SuccessfulLogin(_) =>
        Redirect(routes.MainOzoneCtrl.index()).flashing(("message", "You have been logged in"))
      case NoSuchOrg | WrongPassword =>
        Redirect(routes.MainOzoneCtrl.index()).flashing(("warning", "Wrong credentials"))
    }
  }
}

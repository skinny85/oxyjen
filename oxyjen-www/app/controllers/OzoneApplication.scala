package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._

object OzoneApplication extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.ozone.index())
  }

  case class LoginForm(orgId: String, password: String)

  val loginForm = Form(
    mapping(
      "orgId" -> text,
      "password" -> text
    )(LoginForm.apply)(LoginForm.unapply)
  )

  def login = Action { implicit request =>
    val loginFormViewModel = loginForm.bindFromRequest().get
    OzoneSecurity.login(loginFormViewModel.orgId, loginFormViewModel.password) match {
      case SuccessfulLogin(_) =>
        Redirect(routes.OzoneApplication.index()).flashing(("message", "You have been logged in"))
      case NoSuchOrg | WrongPassword =>
        Redirect(routes.OzoneApplication.index()).flashing(("warning", "Wrong credentials"))
    }
  }
}

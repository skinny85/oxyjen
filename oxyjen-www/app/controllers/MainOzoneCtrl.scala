package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._

object MainOzoneCtrl extends Controller {
  case class LoginViewModel(orgId: String, password: String)

  val loginForm = Form(
    mapping(
      "orgId" -> text,
      "password" -> text
    )(LoginViewModel.apply)(LoginViewModel.unapply)
  )
  
  def index(orgId: String = "") = Action { implicit request =>
    Ok(views.html.ozone.index(loginForm.fill(LoginViewModel(orgId, ""))))
  }

  def login = Action { implicit request =>
    val boundLoginForm = loginForm.bindFromRequest()
    boundLoginForm.fold(
      loginFormWithErrors => {
        BadRequest(views.html.ozone.index(loginFormWithErrors))
      },
      loginFormViewModel => {
        OzoneSecurity.login(loginFormViewModel.orgId, loginFormViewModel.password) match {
          case SuccessfulLogin(_) =>
            Redirect(routes.MainOzoneCtrl.index()).flashing(("message", "You have been logged in"))
          case NoSuchOrg | WrongPassword =>
            Redirect(routes.MainOzoneCtrl.index(loginFormViewModel.orgId)).flashing(("warning", "Wrong credentials"))
        }
      }
    )
  }
}

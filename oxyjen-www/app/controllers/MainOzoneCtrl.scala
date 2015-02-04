package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.CtrlSecurityUtil

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
    val viewParam = CtrlSecurityUtil.loggedIn() match {
      case Some(org) =>
        Left(org)
      case None =>
        Right(loginForm.fill(LoginViewModel(orgId, "")))
    }
    Ok(views.html.ozone.index(viewParam))
  }

  def login = Action { implicit request =>
    val boundLoginForm = loginForm.bindFromRequest()
    boundLoginForm.fold(
      loginFormWithErrors => {
        BadRequest(views.html.ozone.index(Right(loginFormWithErrors)))
      },
      loginFormViewModel => {
        OzoneSecurity.login(loginFormViewModel.orgId, loginFormViewModel.password) match {
          case Some(tksid) =>
            Redirect(routes.MainOzoneCtrl.index())
              .withSession("tksid" -> tksid)
          case None =>
            Redirect(routes.MainOzoneCtrl.index(loginFormViewModel.orgId)).flashing(("warning", "Wrong credentials"))
        }
      }
    )
  }

  def logout = Action { implicit request =>
    CtrlSecurityUtil.logout(routes.MainOzoneCtrl.index())
  }
}

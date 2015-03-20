package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import controllers.util.CtrlSecurityUtil

import models._

object SignInCtrl extends Controller {
  case class LoginViewModel(orgId: String, password: String)

  val loginForm = Form(
    mapping(
      "orgId" -> text,
      "password" -> text
    )(LoginViewModel.apply)(LoginViewModel.unapply)
  )

  def logout = Action { implicit request =>
    CtrlSecurityUtil.logout(routes.MainOzoneCtrl.index())
  }

  def login(orgId: String = "") = Action { implicit request =>
    Ok(views.html.ozone.login(orgId))
  }

  def loginPost = Action { implicit request =>
    val boundLoginForm = loginForm.bindFromRequest()
    boundLoginForm.fold(
      loginFormWithErrors => {
        BadRequest(views.html.ozone.login(loginFormWithErrors("orgId").value.getOrElse("")))
      },
      loginFormViewModel => {
        OzoneSecurity.login(loginFormViewModel.orgId, loginFormViewModel.password) match {
          case Some(tksid) =>
            Redirect(routes.OrganizationCtrl.main())
              .withSession("tksid" -> tksid)
          case None =>
            Redirect(routes.SignInCtrl.login(loginFormViewModel.orgId))
              .flashing(("error", "Incorrect Organization ID and/or password"))
        }
      }
    )
  }
}

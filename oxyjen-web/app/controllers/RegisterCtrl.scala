package controllers

import controllers.util.CtrlFormDataUtil
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._

object RegisterCtrl extends Controller {

  case class RegisterViewModel(orgId: String, password: String, password2: String)

  private val registerForm = Form(
    mapping(
      "orgId" -> text,
      "password" -> text,
      "password2" -> text
    )(RegisterViewModel.apply)(RegisterViewModel.unapply)
  )

  def register = Action { implicit request =>
    Ok(views.html.ozone.register(registerForm))
  }

  def registerPost = Action(implicit request => {
    val boundForm = registerForm.bindFromRequest()
    boundForm.fold(
      formWithErrors => {
        BadRequest(views.html.ozone.register(formWithErrors))
      },
      registerViewModel => {
        if (registerViewModel.password != registerViewModel.password2) {
          var returnForm = boundForm
            .withError(FormError("password2", "Passwords do not match"))
            .fill(registerViewModel.copy(password2 = ""))

          val maybeViolations = OrganizationRepository.validate(registerViewModel.orgId,
            registerViewModel.password)
          if (maybeViolations.isDefined)
            returnForm = CtrlFormDataUtil.addViolations(maybeViolations.get, returnForm)

          Ok(views.html.ozone.register(returnForm))
        } else {
          OrganizationRepository.create(registerViewModel.orgId, registerViewModel.password) match {
            case Left(violations) =>
              Ok(views.html.ozone.register(CtrlFormDataUtil.addViolations(violations, boundForm)))
            case Right(tksid) =>
              Redirect(routes.OrganizationCtrl.main()).withSession("tksid" -> tksid)
          }
        }
      }
    )
  })
}

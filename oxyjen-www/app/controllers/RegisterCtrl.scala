package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models.{ConstraintViolation, Registration}

object RegisterCtrl extends Controller {
  case class RegisterViewModel(orgId: String, password: String, password2: String)

  private val registerForm = Form(
    mapping(
      "orgId" -> text,
      "password" -> text,
      "password2" -> text
    )(RegisterViewModel.apply)(RegisterViewModel.unapply)
  )

  def register = Action {
    Ok(views.html.ozone.register(registerForm))
  }

  def registerPost = Action(implicit request => {
    val boundForm = registerForm.bindFromRequest()
    val registerViewModel = boundForm.get

    val maybeViolations = Registration.validate(registerViewModel.orgId, registerViewModel.password)

    var returnForm = boundForm
    if (maybeViolations.isDefined) {
      for (violation <- maybeViolations.get)
        returnForm = returnForm.withError(translateViolation(violation))
    }
    if (registerViewModel.password != registerViewModel.password2)
      returnForm = returnForm
        .withError(FormError("password2", "Passwords do not match"))
        .fill(registerViewModel.copy(password2 = ""))

    Ok(views.html.ozone.register(returnForm))
  })

  private def translateViolation(violation: ConstraintViolation): FormError =
    FormError(violation.property, violation.message)
}

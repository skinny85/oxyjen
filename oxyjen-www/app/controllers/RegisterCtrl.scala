package controllers

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

  def register = Action {
    Ok(views.html.ozone.register(registerForm))
  }

  def registerPost = Action(implicit request => {
    val boundForm = registerForm.bindFromRequest()
    val registerViewModel = boundForm.get // should always succeed because there are no constraints

    if (registerViewModel.password != registerViewModel.password2) {
      var returnForm = boundForm
        .withError(FormError("password2", "Passwords do not match"))
        .fill(registerViewModel.copy(password2 = ""))

      val maybeViolations = Registration.validate(registerViewModel.orgId,
        registerViewModel.password)
      if (maybeViolations.isDefined)
          returnForm = addViolations(maybeViolations.get, returnForm)

      Ok(views.html.ozone.register(returnForm))
    } else {
      Registration.register(registerViewModel.orgId, registerViewModel.password) match {
        case InvalidArguments(violations) =>
          Ok(views.html.ozone.register(addViolations(violations, boundForm)))
        case SuccessfulRegistration(id) =>
          Redirect(routes.RegisterCtrl.success())
      }
    }
  })

  def success = Action {
    Ok(views.html.ozone.register_success())
  }

  private def addViolations(violations: ConstraintViolations,
                            form: Form[RegisterViewModel]): Form[RegisterViewModel] = {
    var ret = form
    for (violation <- violations)
      ret = ret.withError(translateViolation(violation))
    ret
  }

  private def translateViolation(violation: ConstraintViolation): FormError =
    FormError(violation.property, violation.message)
}

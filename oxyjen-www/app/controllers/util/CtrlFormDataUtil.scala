package controllers.util

import models._
import play.api.data.{FormError, Form}

object CtrlFormDataUtil {
  def addViolations[A](violations: ConstraintViolations,
                       form: Form[A]): Form[A] = {
    var ret = form

    for (violation <- violations)
      if (violation.property.isEmpty)
        ret = ret.withGlobalError(violation.message)
      else
        ret = ret.withError(translateViolation(violation))

    ret
  }

  private def translateViolation(violation: ConstraintViolation): FormError =
    FormError(violation.property, violation.message)
}

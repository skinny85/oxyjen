package object models {
  case class ConstraintViolation(property: String, message: String)

  type ConstraintViolations = Seq[ConstraintViolation]

  object ConstraintViolations {
    def apply(cv: ConstraintViolation, rest: ConstraintViolation*): ConstraintViolations = {
      Seq(cv) ++ Seq(rest: _*)
    }

    def apply(violations: Seq[ConstraintViolation]): Option[ConstraintViolations] = {
      if (violations.isEmpty)
        None
      else
        Some(violations)
    }
  }
}

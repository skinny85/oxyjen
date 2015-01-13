package models

object Registration {
  def register(orgId: String, password: String): Unit = {

  }

  def validate(orgId: String, password: String): Option[ConstraintViolations] = {
    var ret: ConstraintViolations = Seq.empty

    def addOrgIdViolation(message: String ) {
      ret = ret :+ ConstraintViolation("orgId", message)
    }

    if (orgId.isEmpty) {
      addOrgIdViolation("Organization ID cannot be empty")
    } else if (orgId.length < 3) {
      addOrgIdViolation("Organization ID must be at least 3 characters long")
    } else if (orgId.length > 100) {
      addOrgIdViolation("Organization ID can be at most 100 characters long")
    } else if (orgId == "xxxx") {
      addOrgIdViolation("Organization ID can't be 'xxxx', you jackass!")
    }

    def addPasswordViolation(message: String) {
      ret = ret :+ ConstraintViolation("password", message)
    }

    if (password.isEmpty) {
      addPasswordViolation("Password cannot be empty")
    } else if (password.length < 3) {
      addPasswordViolation("Password must be at least 3 characters long")
    }

    ConstraintViolations(ret)
  }
}

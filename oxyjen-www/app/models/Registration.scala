package models

import anorm._
import play.api.db.DB
import play.api.Play.current

object Registration {
  def validate(orgId: String, password: String): Option[ConstraintViolations] = {
    var ret: Seq[ConstraintViolation] = Seq.empty

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
    } else {
      DB.withConnection { implicit c =>
        val result = SQL"SELECT org_id FROM Organization WHERE org_id = $orgId"()
        if (result.nonEmpty)
          addOrgIdViolation("An organization with that ID already exists")
      }
    }

    def addPasswordViolation(message: String) {
      ret = ret :+ ConstraintViolation("password", message)
    }

    if (password.isEmpty) {
      addPasswordViolation("Password cannot be empty")
    } else if (password.length < 3) {
      addPasswordViolation("Password must be at least 3 characters long")
    } else if (password.length > 100) {
      addPasswordViolation("Password can be at most 100 characters long")
    }

    ConstraintViolations(ret)
  }

  def register(orgId: String, password: String): RegistrationResult = {
    validate(orgId, password) match {
      case Some(violations) => InvalidArguments(violations)
      case None =>
        val salt = Crypto.generateSalt()
        val hash = Crypto.bcrypt(password, salt)
        var id: Option[Long] = None
        DB.withConnection { implicit c =>
          id = SQL"""
            INSERT INTO Organization (org_id, salt, password) VALUES
              ($orgId, $salt, $hash)""".executeInsert()
        }
        SuccessfulRegistration(id.get)
    }
  }
}

sealed abstract class RegistrationResult
case class SuccessfulRegistration(id: Long) extends RegistrationResult
case class InvalidArguments(violations: ConstraintViolations) extends RegistrationResult

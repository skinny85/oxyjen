package models

import java.sql.Connection

import anorm._
import play.api.db.DB
import play.api.Play.current

object OrganizationRepository {
  protected[models] def doFind(orgId: String)(implicit c: Connection): Option[Organization] = {
    val rows = SQL"SELECT * FROM Organization WHERE org_id = $orgId"()
    if (rows.isEmpty)
      None
    else {
      val firstRow = rows.head
      Some(Organization(firstRow[Long]("id"), firstRow[String]("org_id"), firstRow[String]("password"), firstRow[String]("salt")))
    }
  }

  def validate(orgId: String, password: String): Option[ConstraintViolations] =
    DB.withConnection(doValidate(orgId, password)(_))

  def create(orgId: String, password: String): OrgCreationResult =
    DB.withConnection(doCreate(orgId, password)(_))

  protected[models] def doValidate(orgId: String, password: String)(implicit c: Connection): Option[ConstraintViolations] = {
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
      val result = SQL"SELECT org_id FROM Organization WHERE org_id = $orgId"()
      if (result.nonEmpty)
        addOrgIdViolation("An organization with that ID already exists")
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

  protected[models] def doCreate(orgId: String, password: String)(implicit c: Connection): OrgCreationResult = {
    doValidate(orgId, password) match {
      case Some(violations) => InvalidArguments(violations)
      case None =>
        val salt = Crypto.generateSalt()
        val hashedPassword = Crypto.bcrypt(password, salt)
        val id: Option[Long] = SQL"""
            INSERT INTO Organization (org_id, salt, password) VALUES
              ($orgId, $salt, $hashedPassword)""".executeInsert()
        SuccessfulOrgCreation(id.get)
    }
  }
}

sealed abstract class OrgCreationResult
case class SuccessfulOrgCreation(id: Long) extends OrgCreationResult
case class InvalidArguments(violations: ConstraintViolations) extends OrgCreationResult

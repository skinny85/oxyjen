package models

import java.sql.Connection

import anorm._
import play.api.db.DB
import play.api.Play.current

object OrganizationRepository {
  def validate(orgId: String, password: String): Option[ConstraintViolations] =
    DB.withConnection(doValidate(orgId, password)(_))

  def create(orgId: String, password: String): Either[ConstraintViolations, String] =
    DB.withConnection(doCreate(orgId, password)(_))

  private val orgIdRegex = """\A[a-zA-Z][a-zA-Z0-9]*(_[a-zA-Z][a-zA-Z0-9]*)*\z""".r

  protected[models] def doValidate(orgId: String, password: String)(implicit c: Connection): Option[ConstraintViolations] = {
    var ret: Seq[ConstraintViolation] = Seq.empty

    def addOrgIdViolation(message: String ) {
      ret = ret :+ ConstraintViolation("orgId", message)
    }
    val trimmedOrgId = orgId.trim

    if (trimmedOrgId.isEmpty) {
      addOrgIdViolation("Organization ID cannot be empty")
    } else if (trimmedOrgId.length < 3) {
      addOrgIdViolation("Organization ID must be at least 3 characters long")
    } else if (trimmedOrgId.length > 100) {
      addOrgIdViolation("Organization ID can be at most 100 characters long")
    } else if (trimmedOrgId.toLowerCase.contains("oxyjen")) {
      addOrgIdViolation("'Oxyjen' is not allowed as a part of an Organization ID")
    } else if (orgIdIsOnReservedList(trimmedOrgId)) {
      addOrgIdViolation("This Organization ID is reserved")
    } else if (orgIdRegex.findFirstMatchIn(trimmedOrgId).isEmpty) {
      addOrgIdViolation("An Organization ID can only consist of words separated by underscores")
    } else {
      val result = SQL"SELECT org_id FROM Organization WHERE org_id = $trimmedOrgId"()
      if (result.nonEmpty)
        addOrgIdViolation("An organization with that ID already exists")
    }

    def orgIdIsOnReservedList(orgId: String): Boolean = {
      Seq("official", "supported", "original") exists(_.equalsIgnoreCase(orgId))
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

  protected[models] def doCreate(orgId: String, password: String)(implicit c: Connection):
      Either[ConstraintViolations, String]= {
    doValidate(orgId, password) match {
      case Some(violations) => Left(violations)
      case None =>
        val salt = Crypto.generateSalt()
        val hashedPassword = Crypto.bcrypt(password, salt)
        SQL"""INSERT INTO Organization (org_id, description, salt, password) VALUES
              ($orgId, '', $salt, $hashedPassword)""".executeUpdate()
        Right(SessionRepository.doCreateSession(orgId))
    }
  }

  protected[models] def doFind(orgId: String)(implicit c: Connection): Option[Organization] = {
    val rows = SQL"SELECT * FROM Organization WHERE org_id = $orgId"()
    if (rows.isEmpty)
      None
    else {
      val firstRow = rows.head
      Some(Organization(firstRow[String]("org_id"), firstRow[String]("description"),
        firstRow[String]("password"), firstRow[String]("salt")))
    }
  }

  def update(org: Organization): Unit = {
    DB.withConnection { implicit c =>
      SQL"UPDATE Organization SET description = ${org.desc}".executeUpdate()
    }
  }
}

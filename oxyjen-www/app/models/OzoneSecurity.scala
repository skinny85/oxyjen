package models

import java.sql.Connection

import anorm._
import play.api.db.DB
import play.api.Play.current

object OzoneSecurity {
  def login(orgId: String, password: String): LoginResult = {
    DB.withConnection(doLogin(orgId, password)(_))
  }

  private def doLogin(orgId: String, password: String)(implicit c: Connection): LoginResult = {
    val maybeOrg = OrganizationRepository.doFind(orgId)
    if (maybeOrg.isEmpty)
      NoSuchOrg
    else {
      val org = maybeOrg.get
      if (verifyPassword(password, org))
        SuccessfulLogin(SessionRepository.doCreateSession(orgId))
      else
        WrongPassword
    }
  }

  private def verifyPassword(password: String, org: Organization): Boolean = {
    Crypto.checkPassword(password, org.hashedPassword)
  }

  def verifyToken(tksid: String): Option[Organization] = {
    SessionRepository.findOrgForSession(tksid)
  }

  def invalidateToken(tksid: String): Boolean = {
    SessionRepository.removeSession(tksid)
  }
}

sealed abstract class LoginResult
case class SuccessfulLogin(tksid: String) extends LoginResult
case object NoSuchOrg extends LoginResult
case object WrongPassword extends LoginResult

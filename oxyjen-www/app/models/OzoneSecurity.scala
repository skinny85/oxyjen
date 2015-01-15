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
      if (Crypto.checkPassword(password, org.hashedPassword))
        SuccessfulLogin("token")
      else
        WrongPassword
    }
  }
}

sealed abstract class LoginResult
case class SuccessfulLogin(token: String) extends LoginResult
case object NoSuchOrg extends LoginResult
case object WrongPassword extends LoginResult

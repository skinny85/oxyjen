package models

import java.sql.Connection

import play.api.db.DB
import play.api.Play.current

object OzoneSecurity {
  def login(orgId: String, password: String): Option[String] = {
    DB.withTransaction(doLogin(orgId, password)(_))
  }

  private def doLogin(orgId: String, password: String)(implicit c: Connection): Option[String] = {
    OrganizationRepository.doFind(orgId).flatMap { org =>
      if (verifyPassword(password, org))
        Some(SessionRepository.doCreateSession(orgId))
      else
        None
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

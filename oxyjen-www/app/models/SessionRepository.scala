package models

import java.sql.Connection
import java.util.Date

import anorm._
import play.api.db.DB
import play.api.Play.current

object SessionRepository {
  def createSession(orgId: String): String = {
    DB.withConnection(doCreateSession(orgId: String)(_))
  }

  protected[models] def doCreateSession(orgId: String)(implicit c: Connection): String = {
    val now = new Date
    val id = Crypto.randomHash()
    SQL"""INSERT INTO Session (id, org_id, active, created, expires, ip_address) VALUES
              ($id, $orgId, TRUE, $now, $now, null)""".executeInsert()
    id
  }
  
  def findOrgForSession(id: String): Option[Organization] = {
    DB.withConnection(doFindOrgForSession(id)(_))
  }
  
  protected[models] def doFindOrgForSession(id: String)(implicit c: Connection): Option[Organization] = {
    val result = SQL"""SELECT * FROM Session s JOIN Organization o ON s.org_id = o.org_id
           WHERE s.id = $id"""()
    if (result.isEmpty)
      None
    else {
      val firstRow = result.head
      Some(Organization(firstRow[Long]("Organization.id"), firstRow[String]("Organization.org_id"),
        firstRow[String]("Organization.password"), firstRow[String]("Organization.salt")))
    }
  }
  
  def removeSession(id: String): Boolean = {
    DB.withConnection(doRemoveSession(id)(_))
  }
  
  private def doRemoveSession(id: String)(implicit c: Connection): Boolean = {
    SQL"DELETE FROM Session WHERE id = $id".executeUpdate() > 0
  }
}

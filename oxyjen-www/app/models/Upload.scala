package models

import java.io.File
import java.sql.Connection

import anorm._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import play.api.libs.ws.{WSAuthScheme, WS}

import scala.None
import scala.util.Failure

object Upload {
  def validate(name: String, version: String): ConstraintViolations =
    DB.withConnection(doValidate(name, version)(_))

  protected[models] def doValidate(name: String, version: String)
                                  (implicit c: Connection): ConstraintViolations = {
    var ret: Seq[ConstraintViolation] = Seq.empty

    def addNameViolation(message: String ) {
      ret = ret :+ ConstraintViolation("name", message)
    }

    if (name.isEmpty) {
      addNameViolation("Name cannot be empty")
    } else if (name.length < 3) {
      addNameViolation("Name must be at least 3 characters long")
    } else if (name.length > 100) {
      addNameViolation("Name can be at most 100 characters long")
    }

    def addVersionViolation(message: String) {
      ret = ret :+ ConstraintViolation("version", message)
    }

    if (version.isEmpty) {
      addVersionViolation("Version cannot be empty")
    } else if (version.length < 3) {
      addVersionViolation("Version must be at least 3 characters long")
    } else if (version.length > 100) {
      addVersionViolation("Version can be at most 100 characters long")
    }

    ret
  }

  def upload(org: Organization, name: String, version: String, archive: File):
      Option[ConstraintViolations] = {
    DB.withConnection(doUpload(org, name, version, archive)(_))
  }

  def doUpload(org: Organization, name: String, version: String, file: File)
              (implicit c: Connection): Option[ConstraintViolations] = {
    val violations = doValidate(name, version)
    if (!violations.isEmpty)
      return Some(violations)

    val holder = WS.
      url(s"http://localhost:8081/artifactory/oxyjen/${org.orgId}/$name/$version/$name-$version.zip").
      withAuth("admin", "password", WSAuthScheme.BASIC)

    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    val result = holder.put(file)
    result.onComplete {
      case Failure(e) =>
        Logger.error("Error uploading file", e)
      case scala.util.Success(resp) =>
        Logger.info("Upload response:\n" + resp.json.as[String])
    }
    result.wait()
    None
  }
}

//sealed abstract class UploadResult
//case class SuccessfulOrgCreation(id: Long) extends UploadResult
//case class InvalidOrgArguments(violations: ConstraintViolations) extends UploadResult

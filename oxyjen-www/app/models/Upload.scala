package models

import java.io.File
import java.sql.Connection
import models.util.Futures

import anorm._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import play.api.http.{Writeable, ContentTypeOf}
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS}

import scala.concurrent.Future
import scala.util.Failure

object Upload {
  def validate(name: String, version: String): ConstraintViolations =
    DB.withConnection(doValidate(name, version)(_))

  protected[models] def doValidate(name: String, version: String)
                                  (implicit c: Connection): ConstraintViolations = {
    var ret: Seq[ConstraintViolation] = Seq.empty

    def addNameViolation(message: String) {
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
  Either[ConstraintViolations, Future[Unit]] = {
    DB.withConnection(doUpload(org, name, version, archive)(_))
  }

  def doUpload(org: Organization, name: String, version: String, archive: File)
              (implicit c: Connection): Either[ConstraintViolations, Future[Unit]] = {
    val violations = doValidate(name, version)
    if (violations.nonEmpty)
      return Left(violations)

    val pomXml =
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>

    <groupId>{org.orgId}</groupId>
    <artifactId>{name}</artifactId>
    <version>{version}</version>
    <packaging>zip</packaging>
    <description>Artifactory auto generated POM</description>
</project>

    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

    def archiveRequest(): Future[WSResponse] = {
      artifactoryPut[File](archive, "zip")(null, null)
    }

    def pomRequest(): Future[WSResponse] = {
      artifactoryPut(pomXml, "pom")
    }

    def artifactoryPut[T](payload: T, ext: String)
                         (implicit wrt: Writeable[T],
                          ct: ContentTypeOf[T]): Future[WSResponse] = {
      val holder = WS.
        url(s"http://localhost:8081/artifactory/oxyjen/${org.orgId}/$name/$version/$name-$version.$ext").
        withAuth("admin", "password", WSAuthScheme.BASIC)

      val result = payload match {
        case file: File => holder.put(file)
        case _ => holder.put(payload)
      }
      result.onComplete {
        case Failure(e) =>
          Logger.error("Error uploading file", e)
        case scala.util.Success(resp) =>
          Logger.info("Upload response:\n" + resp.json.toString())
      }
      result
    }

    val archiveResult: Future[WSResponse] = archiveRequest()
    val pomResult: Future[WSResponse] = pomRequest()

    Right(Futures.all(archiveResult, pomResult))
  }
}

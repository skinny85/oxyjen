package models

import java.io.File
import java.sql.Connection
import java.util.concurrent.TimeUnit
import models.util.Futures

import anorm._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import play.api.http.{Writeable, ContentTypeOf}
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS}
import play.api.libs.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Failure

object Upload {
  def validate(org: Organization, name: String, version: String): ConstraintViolations =
    DB.withTransaction(doValidate(org, name, version)(_))

  protected[models] def doValidate(org: Organization, name: String, version: String)
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

    if (ret.isEmpty) {
      val future = Artifacts.search(org, name, version)
      val result = Await.result(future, Duration(60, TimeUnit.SECONDS)) // lazy fuck
      if (result.nonEmpty)
        ret = ret :+ ConstraintViolation("", "There already exists an artifact with that name and version")
    }

    ret
  }

  def upload(org: Organization, name: String, version: String, archive: File):
      Either[ConstraintViolations, Future[Unit]] = {
    DB.withTransaction(doUpload(org, name, version, archive)(_))
  }

  def doUpload(org: Organization, name: String, version: String, archive: File)
              (implicit c: Connection): Either[ConstraintViolations, Future[Unit]] = {
    val violations = doValidate(org, name, version)
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
      artifactoryPut(archive, "zip")(null, null)
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

      val uploadResult = payload match {
        case file: File => holder.put(file)
        case _ => holder.put(payload)
      }

      val ret = Futures.mapTry(uploadResult) {
        case scala.util.Success(resp) =>
          val maybeErrorsJson = resp.json.asOpt[ArtifactoryErrorsJson]
          maybeErrorsJson match {
            case Some(errors) =>
              throw new IllegalStateException("Error resp from Artifactory: " +
                errors.format)
            case None =>
              resp
          }
        case Failure(t) =>
          throw t
      }
      ret.onComplete {
        case Failure(e) =>
          Logger.error("Error uploading file", e)
        case scala.util.Success(resp) =>
          Logger.info("Upload response:\n" + resp.json.toString())
      }
      ret
    }

    val archiveResult: Future[WSResponse] = archiveRequest()
    val pomResult: Future[WSResponse] = pomRequest()

    Right(Futures.all(archiveResult, pomResult))
  }

  case class ArtifactoryErrorJson(status: Int, message: String) {
    def format: String = s"$message (HTTP response status: $status)"
  }
  case class ArtifactoryErrorsJson(errors: List[ArtifactoryErrorJson]) {
    def format: String = errors.map(_.format).mkString("\n")
  }

  implicit val errorFormat = Json.reads[ArtifactoryErrorJson]
  implicit val errorsFormat = Json.reads[ArtifactoryErrorsJson]
}

package models

import java.io.File

import models.artifactory.ArtifactoryIntegration

import scala.concurrent.Future

object Upload {
  def validate(org: Organization, name: String, version: String,
               file: Option[File] = None): Future[ConstraintViolations] = {
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
      Artifacts.search(org, name, version) map { result =>
        if (result.nonEmpty)
          List(ConstraintViolation("", "There already exists an artifact with that name and version"))
        else
          Nil
      }
    } else {
      Future.successful(ret)
    }
  }

  def upload(org: Organization, name: String, version: String, archive: File):
      Future[Either[ConstraintViolations, Unit]] = {
    validate(org, name, version, Some(archive)) flatMap { violations =>
      if (violations.nonEmpty)
        Future.successful(Left(violations))
      else
        ArtifactoryIntegration.upload(org.orgId, name, version, archive) map (Right(_))
    }
  }

  private implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

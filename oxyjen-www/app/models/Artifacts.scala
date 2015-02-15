package models

import models.artifactory.ArtifactoryIntegration

import scala.concurrent.Future

object Artifacts {
  def search(org: Organization): Future[List[Artifact]] = {
    ArtifactoryIntegration.gavcSearch(groupId = org.orgId)
  }

  def search(org: Organization, name: String, version: String): Future[List[Artifact]] = {
    ArtifactoryIntegration.gavcSearch(groupId = org.orgId, artifactId = name, version = version)
  }

  def search(name: String): Future[List[Artifact]] = {
    ArtifactoryIntegration.nameLike(name)
  }
}

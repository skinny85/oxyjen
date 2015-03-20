package models

import models.artifactory.ArtifactoryIntegration

import scala.concurrent.{ExecutionContext, Future}

object Artifacts {
  def search(org: Organization): Future[List[Artifact]] = {
    ArtifactoryIntegration.gavcSearch(groupId = org.orgId)
  }

  def search(org: Organization, name: String, version: String): Future[List[Artifact]] = {
    ArtifactoryIntegration.gavcSearch(groupId = org.orgId, artifactId = name, version = version)
  }

  def matching(name: String)(implicit ec: ExecutionContext): Future[Map[(String, String), Seq[String]]] = {
    ArtifactoryIntegration.nameMatching(name) map { artifacts =>
      artifacts
        .groupBy(a => (a.groupId, a.name))
        .mapValues(as => as.map(_.version))
    }
  }
}

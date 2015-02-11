package models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS

import scala.concurrent.Future

object Artifacts {
  case class ArtifactorySearchJsonResponse(results: List[SingleResult]) {
    def stringify = results.map(_.stringify)
  }
  case class SingleResult(path: String, created: String) {
    def stringify = s"path=$path, created=" + LocalDateTime.parse(created, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }

  case class Artifact()

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def search(org: Organization): Future[List[String]] = {
    doSearch(s"gavc?g=${org.orgId}&repos=oxyjen")
  }

  def search(org: Organization, name: String, version: String): Future[List[String]] = {
    doSearch(s"gavc?g=${org.orgId}&a=$name&v=$version&repos=oxyjen")
  }

  def search(name: String): Future[List[String]] = {
    doSearch(s"artifact?name=$name")
  }

  private def doSearch(url: String): Future[List[String]] = {
    val fullUrl = "http://localhost:8081/artifactory/api/search/" + url
    val holder = WS.url(fullUrl)
      .withHeaders("X-Result-Detail" -> "info")
    holder.get().map { resp =>
      val result = resp.json.as[ArtifactorySearchJsonResponse]
      result.stringify
    }
  }

  implicit val resultRead = Json.reads[SingleResult]
  implicit val resultsRead = Json.reads[ArtifactorySearchJsonResponse]
}

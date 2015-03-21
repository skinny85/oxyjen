package models.artifactory

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import models.Artifact
import models.util.{PlayConf, Futures}

import play.api.Logger
import play.api.Play.current
import play.api.http.{ContentTypeOf, Writeable}
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSAuthScheme, WSResponse}
import views.html.helper

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ArtifactoryIntegration {
  def gavcSearch(groupId: String = "", artifactId: String = "", version: String = ""):
      Future[List[Artifact]] = {
    if (Seq(groupId, artifactId, version).forall(_.isEmpty))
      throw new IllegalArgumentException("You must specify at least one of groupId, artifactId or version")

    val baseUrl = s"gavc?repos=$repo"
    val groupPart = paramIfNotEmpty(groupId, "g")
    val artifactPart = paramIfNotEmpty(artifactId, "a")
    val versionPart = paramIfNotEmpty(version, "v")
    val url = baseUrl + groupPart + artifactPart + versionPart

    performGetAndParseResults(url)
  }

  def nameMatching(name: String): Future[List[Artifact]] = {
    val escapedName = helper.urlEncode(name)
    performGetAndParseResults(s"artifact?repos=$repo&name=$escapedName")
  }

  def upload(org: String, name: String, version: String, archive: File):
      Future[Unit] = {
    performUpload(org, name, version, archive)
  }

  def getArtifactoryBaseUrl = PlayConf.str("artifactory.url",
    "http://localhost:8081/artifactory/")
  private val repo = "oxyjen"
  private def getArtifactorySearchApiBaseUrl = getArtifactoryBaseUrl + "api/search/"
  private def getArtifactoryUser = PlayConf.str("artifactory.user", "admin")
  private def getArtifactoryPassword = PlayConf.str("artifactory.pw", "password")

  private def paramIfNotEmpty(paramValue: String, paramName: String): String = {
    if (paramValue.isEmpty) "" else s"&$paramName=$paramValue"
  }

  private def performGetAndParseResults(url: String): Future[List[Artifact]] = {
    val fullUrl = getArtifactorySearchApiBaseUrl + url
    val holder = WS.url(fullUrl)
      .withHeaders("X-Result-Detail" -> "info")
    Futures.mapTry(holder.get()) {
      case Success(resp) =>
        resp.json.as[SearchResultsJson].asArtifactsList
      case Failure(t) =>
        throw t
    }
  }

  private case class SearchResultsJson(results: List[EntityInfoJson]) {
    def asArtifactsList = results.filter(_.path.endsWith(".zip")).map(_.asArtifact)
  }

  private case class EntityInfoJson(path: String, created: String) {
    def asArtifact: Artifact = {
        val pathFragments = path.split('/')
        Artifact(pathFragments(1), pathFragments(2), pathFragments(3),
          LocalDateTime.parse(created, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
  }

  private implicit val searchResultsJsonReads = Json.reads[EntityInfoJson]
  private implicit val entityInfoJsonReads = Json.reads[SearchResultsJson]

  private def performUpload(org: String, name: String, version: String, archive: File):
      Future[Unit] = {
    val pomXml =
      <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
               xmlns="http://maven.apache.org/POM/4.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <modelVersion>4.0.0</modelVersion>

        <groupId>{org}</groupId>
        <artifactId>{name}</artifactId>
        <version>{version}</version>
        <packaging>zip</packaging>
        <description>Artifactory auto generated POM</description>
      </project>

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
        url(getArtifactoryBaseUrl + s"$repo/$org/$name/$version/$name-$version.$ext").
        withAuth(getArtifactoryUser, getArtifactoryPassword, WSAuthScheme.BASIC)

      val uploadResult = payload match {
        case file: File => holder.put(file)
        case _ => holder.put(payload)
      }

      val ret = Futures.mapTry(uploadResult) {
        case Success(resp) =>
          val maybeErrorsJson = resp.json.asOpt[ErrorResponseJson]
          maybeErrorsJson match {
            case Some(errors) =>
              throw new IllegalStateException("Error response from Artifactory: " +
                errors.format)
            case None =>
              resp
          }
        case Failure(t) =>
          throw t
      }
      ret.onSuccess { case resp =>
        Logger.info("Upload response:\n" + resp.json.toString())
      }
      ret
    }

    val archiveResult: Future[WSResponse] = archiveRequest()
    val pomResult: Future[WSResponse] = pomRequest()

    Futures.all(archiveResult, pomResult)
  }

  private case class ErrorResponseJson(errors: List[ErrorPartJson]) {
    def format: String = errors.map(_.format).mkString("; ")
  }
  private case class ErrorPartJson(status: Int, message: String) {
    def format: String = s"$message (HTTP response status: $status)"
  }

  private implicit val errorPartJsonReads = Json.reads[ErrorPartJson]
  private implicit val errorResponseJsonReads = Json.reads[ErrorResponseJson]

  private implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

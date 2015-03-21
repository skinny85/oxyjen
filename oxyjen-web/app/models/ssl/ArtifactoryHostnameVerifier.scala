package models.ssl

import javax.net.ssl.{SSLSession, HostnameVerifier}

import models.artifactory.ArtifactoryIntegration
import play.api.libs.ws.ssl.DefaultHostnameVerifier

class ArtifactoryHostnameVerifier extends HostnameVerifier {
  private val dhnv = new DefaultHostnameVerifier

  override def verify(hostname: String, sslSession: SSLSession): Boolean = {
    if (hostname == artifactoryHostname()) {
      true
    } else {
      dhnv.verify(hostname, sslSession)
    }
  }

  private def artifactoryHostname(): String = {
    extractHostname(ArtifactoryIntegration.getArtifactoryBaseUrl)
  }

  private def extractHostname(url: String): String = {
    val afterProtocolIndex = url.indexOf("//") + 2
    url.substring(afterProtocolIndex, url.indexOf('/', afterProtocolIndex))
  }
}

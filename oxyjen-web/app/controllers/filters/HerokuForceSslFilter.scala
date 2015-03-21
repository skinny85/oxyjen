package controllers.filters

import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future

object HerokuForceSslFilter extends Filter {
  override def apply(f: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    val herokuOriginalProtocol = rh.headers.get("X-Forwarded-Proto").getOrElse("https")
    if (Seq("ozone/register", "ozone/login").exists(rh.uri.contains(_))
        && herokuOriginalProtocol != "https") {
      Future.successful(Results.MovedPermanently("https://" + rh.host + rh.uri))
    } else {
      f(rh)
    }
  }
}

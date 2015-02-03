package controllers.util

import play.api.mvc._

import models._

object SecurityUtil {
  def loggedIn()(implicit request: Request[_]): Option[Organization] = {
    getTksid().flatMap(tksid => OzoneSecurity.verifyToken(tksid))
  }

  def logout(call: Call)(implicit request: Request[_]): Result = {
    getTksid() match {
      case None =>
        Results.Redirect(call)
      case Some(tksid) =>
        OzoneSecurity.invalidateToken(tksid)
        Results.Redirect(call).withNewSession
    }
  }

  private def getTksid()(implicit request: Request[_]): Option[String] = {
    request.session.get("tksid")
  }
}

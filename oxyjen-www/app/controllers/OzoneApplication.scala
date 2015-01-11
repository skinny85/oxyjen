package controllers

import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

object OzoneApplication extends Controller {
  def index = Action {
    Ok(views.html.ozone.index())
  }

  val singleForm = Form(
    single(
      "orgId" -> text
    )
  )

  def register = Action {
    Ok(views.html.ozone.register(singleForm))
  }

  def registerPost = Action(implicit request => {
    val userData = singleForm.bindFromRequest()
    val orgId: String = userData.get // no constraints, so this will always succeed
    Logger.info("submitted orgId = '" + orgId + "'")

    val form = if (orgId == "xxxx")
      singleForm.withError(FormError("orgId", "organization ID can't be 'xxxx', you jackass!"))
    else
      singleForm

    Ok(views.html.ozone.register(form))
  })
}

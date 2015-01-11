package controllers

import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

object RegisterCtrl extends Controller {
  val singleForm = Form(
    single(
      "orgId" -> text
    )
  )

  def register = Action {
    Ok(views.html.ozone.register(singleForm))
  }

  def registerPost = Action(implicit request => {
    val boundForm = singleForm.bindFromRequest()
    val orgId: String = boundForm.get // no constraints, so this will always succeed
    Logger.info("submitted orgId = '" + orgId + "'")

    //    val filledForm = singleForm.fill(orgId)
    val form = if (orgId == "xxxx")
      boundForm.withError(FormError("orgId", "organization ID can't be 'xxxx', you jackass!"))
    else
      boundForm

    Ok(views.html.ozone.register(form))
  })
}

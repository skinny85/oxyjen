import play.api.mvc._
import controllers.filters.HerokuForceSslFilter

object Global extends WithFilters(HerokuForceSslFilter)

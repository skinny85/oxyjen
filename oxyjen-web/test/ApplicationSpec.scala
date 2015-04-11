import models.OrganizationRepository
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class ApplicationSpec extends Specification {
  "Application" should {
    "send 404 on a bad request" in new TestUtils.WithMemDbApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new TestUtils.WithMemDbApplication {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Oxyjen")
    }
  }
}

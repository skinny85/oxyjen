import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class FunctionalSpec extends Specification {
  "Application" should {
    "work from within a browser" in new WithBrowser(app = TestUtils.fakeMemDbApplication()) {
      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Oxyjen")
    }
  }
}

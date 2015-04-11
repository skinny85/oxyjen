import play.api.test._
import play.api.test.Helpers._

object TestUtils {
  def fakeMemDbApplication() = FakeApplication(additionalConfiguration = inMemoryDatabase())

  abstract class WithMemDbApplication extends WithApplication(fakeMemDbApplication())
}

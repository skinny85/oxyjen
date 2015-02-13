package models

import org.specs2.matcher.MatchResult
import org.specs2.mutable._

class OrganizationValidationSpec extends Specification {
  "Org ID validation" should {
    "reject an Organization named 'oxyjen' regardless of case" in {
      confirmPasswordIsInvalid("Oxyjen")
    }

    "reject an Organization ID with 'Oxyjen' anywhere in the name" in {
      confirmPasswordIsInvalid("abc_dOxyjeNe")
    }

    "reject an Organization ID with two consecutive underscores" in {
      confirmPasswordIsInvalid("abc__def")
    }

    "reject an Organization ID with some reserved name regardless of case" in {
      confirmPasswordIsInvalid("Official")
    }

    "accept an Organization ID with a reserved word as only part of the name" in {
      confirmPasswordIsValid("Ibm_Official")
    }
  }

  def confirmPasswordIsInvalid(orgId: String): MatchResult[String] = {
    val maybeViolations = OrganizationRepository.doValidate(orgId, "p@sSw0rd")(null)
    maybeViolations.isDefined must beTrue
    val violations = maybeViolations.get
    violations must have size 1
    val violation = violations(0)
    violation.property must be("orgId")
  }

  def confirmPasswordIsValid(orgId: String): MatchResult[Any] = {
    try {
      val maybeViolations = OrganizationRepository.doValidate(orgId, "p@sSw0rd")(null)
      1 === 2 // unreachable
    } catch {
      case _: NullPointerException => 1 === 1
    }
  }
}

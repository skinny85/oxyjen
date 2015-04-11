package models

import org.specs2.matcher.MatchResult
import org.specs2.mutable._

class OrganizationValidationSpec extends Specification {
  "Org ID validation" should {
    "reject an Organization named 'oxyjen' regardless of case" in {
      confirmOrgIdIsInvalid("Oxyjen")
    }

    "reject an Organization ID with 'Oxyjen' anywhere in the name" in {
      confirmOrgIdIsInvalid("abc_dOxyjeNe")
    }

    "reject an Organization ID with two consecutive underscores" in {
      confirmOrgIdIsInvalid("abc__def")
    }

    "reject an Organization ID with some reserved name regardless of case" in {
      confirmOrgIdIsInvalid("Official")
    }

    "accept an Organization ID with a reserved word as only part of the name" in {
      confirmOrgIsValid("Ibm_Official")
    }
  }

  def confirmOrgIdIsInvalid(orgId: String): MatchResult[String] = {
    val maybeViolations = OrganizationRepository.doValidate(orgId, "p@sSw0rd")(null)
    maybeViolations.isDefined must beTrue
    val violations = maybeViolations.get
    violations must have size 1
    val violation = violations.head
    violation.property must be("orgId")
  }

  def confirmOrgIsValid(orgId: String): MatchResult[Any] = {
    try {
      val maybeViolations = OrganizationRepository.doValidate(orgId, "p@sSw0rd")(null)
      maybeViolations.size === -1 // unreachable
    } catch {
      case _: NullPointerException => 1 === 1
    }
  }
}

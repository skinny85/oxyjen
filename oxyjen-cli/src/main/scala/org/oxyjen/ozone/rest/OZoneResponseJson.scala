package org.oxyjen.ozone.rest

sealed abstract class OZoneResponseJson(val status: String, val message: String)

final case class GenericResponseJson(override val status: String,
                                     override val message: String)
  extends OZoneResponseJson(status, message)

final case class InvalidOrgJson(override val status: String, override val message: String,
                                violations: List[String])
  extends OZoneResponseJson(status, message)

final case class OrgCreated(override val status: String, override val message: String,
                             tksid: String) extends OZoneResponseJson(status, message)

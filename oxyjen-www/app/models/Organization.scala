package models

case class Organization(id: Long, orgId: String, hashedPassword: String, salt: String)

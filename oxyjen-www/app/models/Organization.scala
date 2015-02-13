package models

case class Organization(orgId: String, desc: String, hashedPassword: String, salt: String)

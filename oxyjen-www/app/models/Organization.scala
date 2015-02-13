package models

case class Organization(id: Long, orgId: String, desc: String,
                        hashedPassword: String, salt: String)

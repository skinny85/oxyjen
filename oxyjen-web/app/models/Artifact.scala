package models

import java.time.LocalDateTime

case class Artifact(groupId: String, name: String, version: String,
                     createdOn: LocalDateTime)

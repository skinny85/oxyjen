package models.util

import play.api.Play.current

object PlayConf {
  def str(key: String, default: String): String = {
    current.configuration.getString(key).getOrElse(default)
  }
}

package models

import java.security.SecureRandom

import org.mindrot.jbcrypt.BCrypt

object Crypto {
  private val RNG = new SecureRandom

  private val SALTING_ROUNDS = 10

  def generateSalt(): String = {
    BCrypt.gensalt(SALTING_ROUNDS, RNG)
  }

  def bcrypt(password: String, salt: String) = BCrypt.hashpw(password, salt)
}

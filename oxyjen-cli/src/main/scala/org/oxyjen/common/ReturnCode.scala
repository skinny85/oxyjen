package org.oxyjen.common

class ReturnCode private (val code: Int) extends AnyVal

object ReturnCode {
  val Success = new ReturnCode(0)

  val IncorrectNumberOfArguments = new ReturnCode(1)
  val ContradictoryArguments = new ReturnCode(2)
  val ConnectionError = new ReturnCode(3)
  val ClientError = new ReturnCode(4)
  val ServerError = new ReturnCode(5)
  val ValidationFailed = new ReturnCode(6)
  val InvalidCredentials = new ReturnCode(7)
  val SessionRequired = new ReturnCode(8)
}

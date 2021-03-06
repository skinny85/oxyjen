package org.oxyjen

import org.oxyjen.Main.parseTargetDirAndContext
import org.oxyjen.common.ReturnCode
import org.oxyjen.test.AbstractUnitSpec

class MainSpec extends AbstractUnitSpec {
  "Main" should "return error when called without arguments" in {
    Main._main(Seq()) should be (ReturnCode.IncorrectNumberOfArguments)
  }

  it should "parse the target directory as '.' when called with no arguments" in {
    val (targetDir, context) = callParse()
    targetDir should be (".")
    context shouldBe empty
  }

  it should "parse the first argument as the target directory" in {
    val (targetDir, context) = callParse("target")
    targetDir should be ("target")
    context shouldBe empty
  }

  it should "parse the first argument as a key-value pair" in {
    val (targetDir, context) = callParse("key=value")
    targetDir should be (".")
    context should be (Map("key" -> "value"))
  }

  it should "parse further arguments as key-value pairs" in {
    val (targetDir, context) = callParse("target", "k1=v1", "k2=v2")
    targetDir should be ("target")
    context should be (Map("k2" -> "v2", "k1" -> "v1"))
  }

  it should "fail gracefully for third argument not being a key-value pair" in {
    Main._main(Seq("build.sbt", "a", "b")) should be (ReturnCode.ContradictoryArguments)
  }

  it should "fail gracefully for non-existing template file given" in {
    Main._main(Seq("xxx")) should be (ReturnCode.ContradictoryArguments)
  }

  private def callParse(args: String*) = parseTargetDirAndContext(Seq(args: _*))
}

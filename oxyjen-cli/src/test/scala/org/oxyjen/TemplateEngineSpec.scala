package org.oxyjen

import org.oxyjen.test.AbstractUnitSpec

class TemplateEngineSpec extends AbstractUnitSpec {
  "Template Engine" should "eval simple JavaScript" in {
    checkEngineRenders("@{= 1 + 2 }@", new IntJsExecResult(3))
  }

  it can "do complex JS expressions" in {
    checkEngineRenders(
      """@{=
        (function() {
          var A = [1, 2, 3], i, ret = 0;
          for (var i = 0; i < A.length; i++) {
            ret += A[i];
          };
          return ret;
        })();
      }@""", new IntJsExecResult(6))
  }

  it should "ignore the result of the statement block" in {
    checkEngineRenders("@{ 1 }@2", new StringJsExecResult("2"))
  }

  it should "contain an $o2 object with a setFileName(String) method" in {
    checkEngineRenders("@{ $o2.setFileName('test') }@", expectedTargetFile = "test")
  }

  it should "set variables from context" in {
    checkEngineRenders("@{= a + b }@",
      new IntJsExecResult(46),
      context = Map("a" -> 12, "b" -> 34))
  }

  it should "does not convert variables from Strings to JavaScript types" in {
    checkEngineRenders("@{= a + b }@",
      new StringJsExecResult("1234"),
      context = Map("a" -> "12", "b" -> "34"))
  }

  private def checkEngineRenders(template: String,
                                 expectedOutput: JsExecResult = new StringJsExecResult(""),
                                 expectedTargetFile: String = "",
                                 context: Map[String, Any] = Map.empty) {
    val result = TemplateEngine.applyTemplate(template, new OxyjenContext(null, context))
    result match {
      case SuccessfulApplication(actualOutput, actualTargetFile) =>
        expectedOutput.assertMatches(actualOutput)
        if (!expectedTargetFile.isEmpty)
          actualTargetFile should be (expectedTargetFile)
      case _ =>
        fail("Unsuccessful script execution: " + result)
    }
  }

  private sealed trait JsExecResult {
    def assertMatches(actualOutput: String): Unit
  }

  private class StringJsExecResult(expectedOutput: String) extends JsExecResult {
    override def assertMatches(actualOutput: String): Unit = {
      actualOutput should be(if (expectedOutput.isEmpty) "" else expectedOutput + "\n")
    }
  }

  /** This class is needed, as sometimes the JS engine converts Ints to Doubles
    * (so, returns '6.0' instead of just '6').
    * @param expectedOutput the integer value that should be the result of
    *                        evaluating the JS code
    */
  private class IntJsExecResult(expectedOutput: Int) extends JsExecResult {
    override def assertMatches(actualOutput: String): Unit = {
      val outputDouble = actualOutput.toDouble
      if (!isInt(outputDouble))
        fail(s"Expected $expectedOutput, got $outputDouble instead")

      outputDouble.toInt should be (expectedOutput)
    }

    private def isInt(double: Double) = Math.rint(double) == double
  }
}

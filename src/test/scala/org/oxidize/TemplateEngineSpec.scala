package org.oxidize

import org.oxidize.test.AbstractUnitSpec

class TemplateEngineSpec extends AbstractUnitSpec {
  "Template Engine" should "eval simple JavaScript" in {
    checkEngineRenders("@{= 1 + 2 }@", "3")
  }

  it can "do complex JS expressions" in {
    checkEngineRenders(
      """@{=
        |(function() {
        |  var A = [1, 2, 3], i, ret = 0;
        |  for (var i = 0; i < A.length; i++) {
        |    ret += A[i];
        |  };
        |  return ret;
        |})();
      }@""".stripMargin, "6")
  }

  it should "ignore the result of the statement block" in {
    checkEngineRenders("@{ 1 }@2", "2")
  }

  it should "contain an $oxidize object with a setFileName(String) method" in {
    checkEngineRenders("@{ $oxidize.setFileName('test') }@", expectedFile = "test")
  }

  it should "set variables from context" in {
    checkEngineRenders("@{= a + b }@", "46", context = Map("a" -> 12, "b" -> 34))
  }

  it should "does not convert variables from Strings to JavaScript types" in {
    checkEngineRenders("@{= a + b }@", "1234", context = Map("a" -> "12", "b" -> "34"))
  }

  private def checkEngineRenders(template: String,
                                 expectedOutput: String = "",
                                 expectedFile: String = "",
                                 context: Map[String, Any] = Map.empty) {
    val result = TemplateEngine.applyTemplate(template, new OxidizeContext(null, context))
    result.output should be(expectedOutput)
    if (!expectedFile.isEmpty)
      result.targetFile should be (expectedFile)
  }
}

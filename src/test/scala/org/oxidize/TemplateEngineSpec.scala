package org.oxidize

import org.oxidize.test.AbstractUnitSpec

class TemplateEngineSpec extends AbstractUnitSpec {
  "Template Engine" should "eval simple JavaScript" in {
    Main.applyTemplate("@{= 1 + 2 }@") should be("3")
  }
}

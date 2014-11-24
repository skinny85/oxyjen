package org.oxidize

import org.oxidize.test.AbstractUnitSpec

class MainSpec extends AbstractUnitSpec {
  "Main" should "return error when called without arguments" in {
    Main._main(Seq()) should be (1)
  }

  it should "not overwrite an existing file" in {
    // later
  }
}

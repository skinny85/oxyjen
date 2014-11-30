package org.oxidize

import java.io.File

import org.apache.commons.io.FileUtils
import org.oxidize.test.AbstractUnitSpec

class MainIntegrationSpec extends AbstractUnitSpec {
  "Main" should "generate a pom according to template" in {
    val testDir = "target/pom-test"
    ensureTestDirIsEmpty(testDir)
    val template =
      """|@{ $oxidize.setFileName('pom.xml') }@
        |<project>
        |    <modelVersion>4.0.0</modelVersion>
        |    <artifactId>artifactId</artifactId>
        |    <groupId>
        |        @{=
        |            var prefix = 'pre.';
        |            prefix + groupId + ".post"
        |        }@
        |    </groupId>
        |    <version>1.0</version>
        |</project>
        |""".stripMargin
    val templateFilePath = testDir + "/pom.ejs.xml"
    FileUtils.writeStringToFile(new File(templateFilePath), template)

    Main._main(Seq(templateFilePath, testDir, "groupId=com.example.js")) should be (0)

    val output = FileUtils.readFileToString(new File(testDir + "/pom.xml"))
    output should be(
      """|<project>
        |    <modelVersion>4.0.0</modelVersion>
        |    <artifactId>artifactId</artifactId>
        |    <groupId>
        |        pre.com.example.js.post
        |    </groupId>
        |    <version>1.0</version>
        |</project>
        |""".stripMargin)
  }

  private def ensureTestDirIsEmpty(path: String): Unit = {
    val testDir = new File(path)
    if (testDir.exists()) {
      FileUtils.deleteDirectory(testDir)
    }
    testDir.mkdirs()
  }
}

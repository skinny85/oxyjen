package org.oxidize

import java.io.File

import org.apache.commons.io.FileUtils
import org.oxidize.test.AbstractUnitSpec

class MainIntegrationSpec extends AbstractUnitSpec {
  "Main" should "generate a POM according to template" in {
    val testDir = "target/pom-test"
    checkOxidizer(testDir = testDir,
      template =
        """@{ $oxidize.setFileName('pom.xml') }@
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
          |""".stripMargin,
      outDir = testDir,
      outFile = Some("pom.xml"),
      expected =
        """<project>
          |    <modelVersion>4.0.0</modelVersion>
          |    <artifactId>artifactId</artifactId>
          |    <groupId>
          |        pre.com.example.js.post
          |    </groupId>
          |    <version>1.0</version>
          |</project>
          |""".stripMargin,
      "groupId=com.example.js")
  }

  private def checkOxidizer(testDir: String,
                    template: String,
                    outDir: String,
                    outFile: Option[String],
                    expected: String,
                    args: String*) {
    ensureTestDirIsEmpty(testDir)

    val templateFilePath = testDir + "/template.txt"
    FileUtils.writeStringToFile(new File(templateFilePath), template)

    Main._main(Seq(templateFilePath, outDir) ++ Seq(args: _*)) should be(0)

    val outFileName = outFile match {
      case Some(o) => o
      case None => "template.txt"
    }
    val output = FileUtils.readFileToString(new File(outDir + "/" + outFileName))
    output should be(expected)
  }

  private def ensureTestDirIsEmpty(path: String): Unit = {
    val testDir = new File(path)
    if (testDir.exists()) {
      FileUtils.deleteDirectory(testDir)
    }
    testDir.mkdirs()
  }
}

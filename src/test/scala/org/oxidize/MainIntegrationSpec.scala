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

  it should "create the output directory when it doesn't exist" in {
    val testDir = "target/new-dir-test"
    checkOxidizer(testDir = testDir,
      template = "@{= a + b }@\n",
      outDir = testDir + "/a/b",
      outFile = None,
      expected = "1234\n",
      "a=12", "b=34")
  }

  it should "allow setting target directory in the script" in {
    val testDir = "target/out-dir-test"
    checkOxidizer(testDir = testDir,
      template =
        """abc reversed is '@{= 'abc'.split('').reverse().join('') }@'.
          |@{
          |  var parts = groupId.split(".");
          |  $oxidize.setFileName(parts.join("/") + "/" + fileName);
          |}@
          |""".stripMargin,
      outDir = testDir,
      outFile = Some("a/b/c/output.txt"),
      expected = "abc reversed is 'cba'.\n",
      "groupId=a.b.c", "fileName=output.txt")
  }

  it should "call all templates recursively when directory is given as argument" in {
    val testDir = "target/recur-test"
    ensureTestDirIsEmpty(testDir)
    val template1 = testDir + "/" + "templ1.txt"
    FileUtils.writeStringToFile(new File(template1), "@{= 1 + 2 }@")
    val template2 = testDir + "/" + "templ2.txt"
    FileUtils.writeStringToFile(new File(template2), "3 + 4")
    val outDir = testDir + "/out"

    Main._main(Seq(testDir, outDir)) should be (0)
    FileUtils.readFileToString(new File(outDir + "/templ1.txt")) should be ("3\n")
    FileUtils.readFileToString(new File(outDir + "/templ2.txt")) should be ("3 + 4\n")
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

package org.oxyjen

import java.io.File

import org.apache.commons.io.FileUtils
import org.oxyjen.test.AbstractUnitSpec

class MainIntegrationSpec extends AbstractUnitSpec {
  val testsTmpDir = "target/integration-test-tmp/"

  "Main" should "generate a POM according to template" in {
    val testDir = testsTmpDir + "pom-test"
    callAndVerifyGeneration(testDir = testDir,
      template =
        """@{ $o2.setFileName('pom.xml') }@
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
      inFile = "template.txt",
      mainFirstArg = None,
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
    val testDir = testsTmpDir + "new-dir-test"
    callAndVerifyGeneration(testDir = testDir,
      template = "@{= a + b }@\n",
      inFile = "template.txt",
      mainFirstArg = None,
      outDir = testDir + "/a/b",
      outFile = None,
      expected = "1234\n",
      "a=12", "b=34")
  }

  it should "allow setting target directory in the script" in {
    val testDir = testsTmpDir + "out-dir-test"
    callAndVerifyGeneration(testDir = testDir,
      template =
        """abc reversed is '@{= 'abc'.split('').reverse().join('') }@'.
          |@{
          |  var parts = groupId.split(".");
          |  $o2.setFileName(parts.join("/") + "/" + fileName);
          |}@
          |""".stripMargin,
      inFile = "template.txt",
      mainFirstArg = None,
      outDir = testDir,
      outFile = Some("a/b/c/output.txt"),
      expected = "abc reversed is 'cba'.\n",
      "groupId=a.b.c", "fileName=output.txt")
  }

  it should "call all templates recursively when directory is given as argument" in {
    val testDir = testsTmpDir + "recur-test"
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

  it should "preserve the original location of the file" in {
    val testDir = testsTmpDir + "flatten-test"
    callAndVerifyGeneration(testDir = testDir,
      template =
        """
          |abc
          |
          |""".stripMargin,
      inFile = "META-INF/manifest.mf",
      mainFirstArg = Some(testDir),
      outDir = testDir + "/out",
      outFile = Some("META-INF/manifest.mf"),
      expected = "abc\n")
  }

  private def callAndVerifyGeneration(testDir: String,
                                      template: String,
                                      inFile: String,
                                      mainFirstArg: Option[String],
                                      outDir: String,
                                      outFile: Option[String],
                                      expected: String,
                                      args: String*) {
    ensureTestDirIsEmpty(testDir)

    val templateFilePath = testDir + "/" + inFile
    FileUtils.writeStringToFile(new File(templateFilePath), template)

    Main._main(Seq(mainFirstArg.getOrElse(templateFilePath), outDir) ++ Seq(args: _*)) should be(0)

    val output = FileUtils.readFileToString(new File(outDir + "/" +
      outFile.getOrElse(inFile)))
    output should be (expected)
  }

  private def ensureTestDirIsEmpty(path: String): Unit = {
    val testDir = new File(path)
    if (testDir.exists()) {
      FileUtils.deleteDirectory(testDir)
    }
    testDir.mkdirs()
  }
}

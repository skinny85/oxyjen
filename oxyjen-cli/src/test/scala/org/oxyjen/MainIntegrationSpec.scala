package org.oxyjen

import java.io.File

import org.apache.commons.io.FileUtils
import org.oxyjen.common.ReturnCode
import org.oxyjen.test.AbstractUnitSpec

class MainIntegrationSpec extends AbstractUnitSpec {
  val testsTmpDir = "target/integration-test-tmp/"

  "Main" should "generate a POM according to template" in {
    val testDir = testsTmpDir + "pom-test"
    writeTemplateAndGenerate(testDir = testDir,
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
      templateFileName = "template.txt",
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
    writeTemplateAndGenerate(testDir = testDir,
      template = "@{= a + b }@\n",
      templateFileName = "template.txt",
      mainFirstArg = None,
      outDir = testDir + "/a/b",
      outFile = None,
      expected = "1234\n",
      "a=12", "b=34")
  }

  it should "allow setting target directory in the script" in {
    val testDir = testsTmpDir + "out-dir-test"
    writeTemplateAndGenerate(testDir = testDir,
      template =
        """abc reversed is '@{= 'abc'.split('').reverse().join('') }@'.
          |@{
          |  var parts = groupId.split(".");
          |  $o2.setFileName(parts.join("/") + "/" + fileName);
          |}@
          |""".stripMargin,
      templateFileName = "template.txt",
      mainFirstArg = None,
      outDir = testDir,
      outFile = Some("a/b/c/output.txt"),
      expected = "abc reversed is 'cba'.\n",
      "groupId=a.b.c", "fileName=output.txt")
  }

  it should "call all templates recursively when directory is given as argument" in {
    val testDir = testsTmpDir + "recur-test/"
    ensureTestDirIsEmpty(testDir)
    FileUtils.writeStringToFile(new File(testDir + "templ1.txt"), "@{= 1 + 2 }@")
    FileUtils.writeStringToFile(new File(testDir + "templ2.txt"), "3 + 4")

    val outDir = testDir + "/out/"
    Main._main(Seq(testDir, outDir)) should be (ReturnCode.Success)

    fileContentsShouldBe(outDir + "templ1.txt", "3\n")
    fileContentsShouldBe(outDir + "templ2.txt", "3 + 4\n")
  }

  it should "preserve the original location of the file" in {
    val testDir = testsTmpDir + "flatten-test"
    writeTemplateAndGenerate(testDir = testDir,
      template =
        """
          |abc
          |
          |""".stripMargin,
      templateFileName = "META-INF/manifest.mf",
      mainFirstArg = Some(testDir),
      outDir = testDir + "/out",
      outFile = Some("META-INF/manifest.mf"),
      expected = "abc\n")
  }

  it should "generate correct Maven project" in {
    val testDir = testsTmpDir + "maven-test"
    ensureTestDirIsEmpty(testDir)
    val inDir = testDir + "/in/"
    val outDir = testDir + "/out/"

    val pomFormat =
      """<project>
        |    <modelVersion>4.0.0</modelVersion>
        |    <groupId>%s</groupId>
        |    <artifactId>%s</artifactId>
        |    <version>1.0</version>
        |    <packaging>jar</packaging>
        |
        |    <dependencies>
        |      <dependency>
        |        <groupId>junit</groupId>
        |        <artifactId>junit</artifactId>
        |        <version>4.11</version>
        |      </dependency>
        |    </dependencies>
        |</project>
        |""".stripMargin
    FileUtils.writeStringToFile(
      new File(inDir + "pom.xml"),
      String.format(pomFormat, "@{= groupId }@", "@{= projectId }@"))

    val manifest =
      """Manifest-Version: 1.0
        |Created-By: 1.3.1 (Apple Computer, Inc.)
        |""".stripMargin
    FileUtils.writeStringToFile(
      new File(inDir + "META-INF/MANIFEST.MF"),
      manifest)

    val classFormat =
      """%spackage %s;
        |
        |public class ExampleClass {
        |}
        |""".stripMargin
    FileUtils.writeStringToFile(
      new File(inDir + "src/main/java/ExampleClass.java"),
      String.format(classFormat,
        """@{
          |        var pathParts = groupId.split('.');
          |        $o2.setFileDir(pathParts.join('/'));
          |}@
          |""".stripMargin,
        "@{= groupId }@"))


    val testFormat =
      """%spackage %s;
        |
        |import org.junit.Test;
        |import org.junit.Assert;
        |
        |public class ExampleTest {
        |    @Test
        |    public void example_test() {
        |        Assert.assertEquals(3, 1 + 2);
        |    }
        |}
        |""".stripMargin
    FileUtils.writeStringToFile(
      new File(inDir + "src/test/java/ExampleTest.java"),
      String.format(testFormat,
        """@{
          |        var pathParts = groupId.split('.');
          |        $o2.setFileName(pathParts.join('/') + '/ExampleTest.java');
          |}@
          |""".stripMargin,
        "@{= groupId }@"))

    Main._main(Seq(inDir, outDir, "groupId=org.example", "projectId=example")) should be (ReturnCode.Success)

    fileContentsShouldBe(outDir + "META-INF/MANIFEST.MF", manifest)
    fileContentsShouldBe(outDir + "pom.xml",
      String.format(pomFormat, "org.example", "example"))
    fileContentsShouldBe(outDir + "src/main/java/org/example/ExampleClass.java",
      String.format(classFormat, "", "org.example"))
    fileContentsShouldBe(outDir + "src/test/java/org/example/ExampleTest.java",
      String.format(testFormat, "", "org.example"))
  }

  it should "handle missing context argument gracefully" in {
    val testDir = testsTmpDir + "missing-ctx-arg"
    ensureTestDirIsEmpty(testDir)
    val templateFile = new File(testDir, "a.txt")
    FileUtils.writeStringToFile(templateFile, "@{= a }@")
    Main._main(Seq(templateFile.getPath)) should be (ReturnCode.MissingScriptArg)
  }

  it should "not confuse missing context arg with using an unknown function" in {
    val testDir = testsTmpDir + "missing-function"
    ensureTestDirIsEmpty(testDir)
    val templateFile = new File(testDir, "f.txt")
    FileUtils.writeStringToFile(templateFile, "@{= a() }@")
    Main._main(Seq(templateFile.getPath, "a=b")) should be (ReturnCode.ErrorInScript)
  }

  it should "not overwrite an existing file" in {
    val testDir = testsTmpDir + "existing-file/in"
    val templateFileName = "a.txt"
    val outDir = testsTmpDir + "existing-file/out"
    val outFile = templateFileName
    FileUtils.writeStringToFile(new File(s"$outDir/$outFile"), "b")
    writeTemplateAndGenerate(
      testDir = testDir,
      template = "c",
      templateFileName = templateFileName,
      mainFirstArg = None,
      outDir = outDir,
      outFile = Some(outFile),
      expected = "b"
    )
  }

  private def writeTemplateAndGenerate(testDir: String,
                                      template: String,
                                      templateFileName: String,
                                      mainFirstArg: Option[String],
                                      outDir: String,
                                      outFile: Option[String],
                                      expected: String,
                                      args: String*) {
    ensureTestDirIsEmpty(testDir)

    val templateFilePath = testDir + "/" + templateFileName
    FileUtils.writeStringToFile(new File(templateFilePath), template)

    Main._main(Seq(mainFirstArg.getOrElse(templateFilePath), outDir) ++ Seq(args: _*)) should be(ReturnCode.Success)

    fileContentsShouldBe(outDir + "/" + outFile.getOrElse(templateFileName), expected)
  }

  private def ensureTestDirIsEmpty(path: String): Unit = {
    val testDir = new File(path)
    if (testDir.exists()) {
      FileUtils.deleteDirectory(testDir)
    }
    testDir.mkdirs()
  }

  def fileContentsShouldBe(filePath: String, expected: String) {
    FileUtils.readFileToString(new File(filePath)) should be(expected)
  }
}

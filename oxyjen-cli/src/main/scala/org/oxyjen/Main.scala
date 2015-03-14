package org.oxyjen

import java.io.File
import java.nio.file.Files
import javax.script.ScriptException

import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.FileUtils
import org.oxyjen.common.{ReturnCode, StdIo}
import org.oxyjen.ivy.IvyResolver

object Main {
  val USAGE = "Usage: o2 TEMPLATE [TARGET_DIR] [NAME=VALUE]..."

  def main(args: Array[String]) {
    val code = _main(args)
    if (code.code != 0)
      System exit code.code
  }

  def _main(args: Seq[String]): ReturnCode = {
    if (args.length < 1) {
      StdIo pute USAGE
      return ReturnCode.IncorrectNumberOfArguments
    }

    try {
      val template = getTemplatePath(args(0))
      val (targetDir, context) = parseTargetDirAndContext(args.slice(1, args.size))
      applyTemplate(template, targetDir, context)
      ReturnCode.Success
    } catch {
      case e@(_: TemplateMissing | _: ArgNotAKeyValPair) =>
        StdIo pute e.getMessage
        ReturnCode.ContradictoryArguments
      case e: MissingValueError =>
        StdIo.pute("Missing required argument '{}'. Supply a value for it on the command line like so: {}=<value>", e.name, e.name)
        ReturnCode.MissingScriptArg
      case e: ScriptError =>
        StdIo.pute("There was an error executing the script ({}). This is most likely a mistake in the template definition itself. We apologize for the inconvenience", e.getMessage)
        ReturnCode.ErrorInScript
    }
  }

  private def getTemplatePath(arg: String): String = {
    def looksLikeExternalDependency(arg: String) = arg.contains(":")

    def deleteOnExit(tempDir: File) {
      Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
          def run(): Unit = {
            FileUtils.deleteDirectory(tempDir)
          }
        }
      ))
    }

    if (looksLikeExternalDependency(arg)) {
      val parts = arg.split(":")
      val groupId = if (parts(0).isEmpty) "oxyjen" else parts(0)
      val artifactId = parts(1)
      val version = if (parts.length > 2) parts(2) else "latest.release"
      val artifact = IvyResolver.resolve(groupId, artifactId, version)
      if (artifact.isEmpty)
        throw new TemplateMissing(groupId, artifactId, version)
      val tempDir = Files.createTempDirectory("oxyjen").toFile
      deleteOnExit(tempDir)
      val zipFile = new ZipFile(artifact.get)
      zipFile.extractAll(tempDir.getPath)
      tempDir.getPath
    } else {
      val file = new File(arg)
      if (!file.exists())
        throw new TemplateMissing(file)
      arg
    }
  }

  def parseTargetDirAndContext(args: Seq[String]): (String, Map[String, Any]) = {
    def looksLikeTargetDir(arg: String) = !arg.contains('=')
    def createContext(strings: Seq[String]) = strings.foldLeft(Map.empty[String, Any])((m, s) => {
      val parts = s.split('=')
      if (parts.length != 2)
        throw new ArgNotAKeyValPair(s)
      else
        m + ((parts(0), parts(1)))
    })

    if (args.isEmpty) {
      (".", Map.empty[String, Any])
    } else {
      if (looksLikeTargetDir(args(0))) {
        (args(0), createContext(args.slice(1, args.size)))
      } else {
        (".", createContext(args))
      }
    }
  }

  def applyTemplate(templatePath: String, targetDir: String, context: Map[String, Any]) {
    val templateFile = new File(templatePath)
    if (templateFile.isDirectory) {
      for (subfile <- templateFile.listFiles())
        applyTemplate2(subfile.getPath, targetDir, context)
    } else {
      applyTemplateToNonDirFile(templateFile, targetDir, context)
    }
  }

  private def applyTemplate2(templatePath: String, targetDir: String, context: Map[String, Any]) {
    val templateFile = new File(templatePath)
    if (templateFile.isDirectory) {
      for (subfile <- templateFile.listFiles())
        applyTemplate2(subfile.getPath, targetDir + "/" + templateFile.getName, context)
    } else {
      applyTemplateToNonDirFile(templateFile, targetDir, context)
    }
  }

  private def applyTemplateToNonDirFile(templateFile: File, targetDir: String, context: Map[String, Any]) {
    val template = FileUtils.readFileToString(templateFile)
    TemplateEngine.applyTemplate(template, new OxyjenContext(templateFile.getName, context)) match {
      case SuccessfulApplication(output, targetFile) =>
        val outFile = new File(targetDir, targetFile)
        FileUtils.writeStringToFile(outFile, output)
      case MissingValueInContext(name) =>
        throw new MissingValueError(name)
      case ScriptExecutionFailure(e) =>
        throw new ScriptError(e)
    }
  }

  private class TemplateMissing private[this] (msg: String) extends
      Exception(msg) {
    def this(groupId: String, name: String, version: String) =
      this(s"Could not find template '$groupId:$name:$version'. Could there be a typo in the name(s)?")
    def this(file: File) =
      this(s"File '${file.getName}' does not exist")
  }

  private class ArgNotAKeyValPair(arg: String) extends
    Exception(s"Argument is not a 'key=value' pair: '$arg'")

  private class MissingValueError(val name: String) extends
    Exception

  private class ScriptError(e: ScriptException) extends
    Exception(e.getMessage)
}

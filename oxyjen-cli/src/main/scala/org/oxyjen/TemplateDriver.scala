package org.oxyjen

import java.io.{FilenameFilter, File}
import javax.script.ScriptException

import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.FileUtils
import org.oxyjen.common.{FileHelper, StdIo, ReturnCode}
import org.oxyjen.ivy.IvyResolver

object TemplateDriver {
  def applyTemplateExtractingItIfNeccessary(templatePathOrId: String, targetDir: String,
                                            params: Seq[String])
                                           (successBlock: => ReturnCode): ReturnCode = {
    try {
      val template = getTemplateRootPath(templatePathOrId)
      val context = createContext(params)
      applyTemplate(template, targetDir, context)
      successBlock
    } catch {
      case e@(_: TemplateMissing | _: ArgNotAKeyValPair) =>
        StdIo pute e.getMessage
        ReturnCode.ContradictoryArguments
      case e: NotJava8 =>
        StdIo pute "You need Java 8 in order to use Oxyjen. Please download the appropriate version from oracle.com and put it on your path (or point JAVA_HOME to it)."
        ReturnCode.InvalidEnvironment
      case e: MissingValueError =>
        StdIo.pute("Missing required argument '{}'. Supply a value for it on the command line like so: {}=<value>", e.name, e.name)
        ReturnCode.MissingScriptArg
      case e: ScriptError =>
        StdIo.pute("There was an error executing the script ({}). This is most likely a mistake in the template definition itself. We apologize for the inconvenience", e.getMessage)
        ReturnCode.ErrorInScript
    }
  }

  private def getTemplateRootPath(arg: String): String = {
    def looksLikeExternalDependency(arg: String) = arg.contains(":")

    if (looksLikeExternalDependency(arg)) {
      val parts = arg.split(":")
      val groupId = if (parts(0).isEmpty) "oxyjen" else parts(0)
      val artifactId = parts(1)
      val version = if (parts.length > 2) parts(2) else "latest.release"
      val artifact = IvyResolver.resolve(groupId, artifactId, version)
      if (artifact.isEmpty)
        throw new TemplateMissing(groupId, artifactId, version)
      val tempDir = FileHelper.transientTempDir("oxyjen")
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

  private def createContext(strings: Seq[String]) = strings.foldLeft(Map.empty[String, Any])((m, s) => {
    val parts = s.split('=')
    if (parts.length != 2)
      throw new ArgNotAKeyValPair(s)
    else
      m + ((parts(0), parts(1)))
  })

  private def applyTemplate(templatePath: String, targetDir: String, context: Map[String, Any]) {
    val templateFile = new File(templatePath)
    if (templateFile.isDirectory) {
      val metaFile = new File(templateFile, "oxyjen.js")
      val resultContext = if (metaFile.exists()) {
        TemplateEngine.executeMetaFile(FileUtils.readFileToString(metaFile)) match {
          case NashornNotFound => throw new NotJava8
          case ScriptExecutionFailure(e) => throw new ScriptError(e)
          case TemplateDescriptor(params) =>
            val missingParams = params.filterKeys(!context.contains(_))
            var resultContext = context
            for ((param, desc) <- missingParams) {
              StdIo puts s"The template requires an argument named '$param'. Description: $desc"
              val value = StdIo.readLine(s"Please provide a value for parameter '$param': ")
              resultContext = resultContext + ((param, value))
            }
            resultContext
        }
      } else {
        context
      }

      for (subfile <- templateFile.listFiles(MetaFilter))
        applyTemplate2(subfile.getPath, targetDir, resultContext)
    } else {
      applyTemplateToNonDirFile(templateFile, targetDir, context)
    }
  }

  private object MetaFilter extends FilenameFilter {
    override def accept(dir: File, name: String): Boolean = name != "oxyjen.js"
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
        if (outFile.exists)
          StdIo puts s"File '${outFile.getPath}' already exists - skipping to not overwrite"
        else
          FileUtils.writeStringToFile(outFile, output)
      case NashornNotFound =>
        throw new NotJava8
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

  private class NotJava8 extends Exception

  private class MissingValueError(val name: String) extends
    Exception

  private class ScriptError(e: ScriptException) extends
    Exception(e.getMessage)
}

package org.oxyjen

import java.io.File

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

object Main {
  private val errLog = LoggerFactory.getLogger("org.oxyjen.Main")

  val USAGE = "Usage: o2 TEMPLATE [TARGET_DIR] [NAME=VALUE]..."

  def main(args: Array[String]) {
    val code = _main(args)
    if (code != 0)
      System exit code
  }

  def _main(args: Seq[String]): Int = {
    if (args.length < 1) {
      errLog warn USAGE
      return 1
    }
    val template = args(0)
    val (targetDir, context) = parseTargetDirAndContext(args.slice(1, args.size))

    applyTemplate(template, targetDir, context)
    0
  }

  def parseTargetDirAndContext(args: Seq[String]): (String, Map[String, Any]) = {
    def looksLikeTargetDir(arg: String) = !arg.contains('=')
    def createContext(strings: Seq[String]) = strings.foldLeft(Map.empty[String, Any])((m, s) => {
      val parts = s.split('=')
      if (parts.length != 2)
        throw new IncorrectCliArgs(s)
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
    val result = TemplateEngine.applyTemplate(template,
      new OxyjenContext(templateFile.getName, context))

    val outFile = new File(targetDir, result.targetFile)
    FileUtils.writeStringToFile(outFile, result.output)
  }
}

class IncorrectCliArgs(arg: String) extends
  Exception(s"Argument is not a 'key=value' pair: '$arg'")

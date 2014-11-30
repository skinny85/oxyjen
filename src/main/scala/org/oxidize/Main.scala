package org.oxidize

import java.io.File

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

object Main {
  private val errLog = LoggerFactory.getLogger("org.oxidize.Main")

  val USAGE = "Usage: oxidize TEMPLATE [TARGET_DIR] [NAME=VALUE]..."

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
    val template = FileUtils.readFileToString(new File(templatePath))
    val result = TemplateEngine.applyTemplate(template, new OxidizeContext(templatePath, context))
    // TODO this is wrong. TemplatePath can be a path, while OxidizeContext should take
    // a simple file name. This needs to be checked

    val outFile = new File(targetDir, result.targetFile)
    FileUtils.writeStringToFile(outFile, result.output)
  }
}

class IncorrectCliArgs(arg: String) extends
  Exception(s"Argument is not a 'key=value' pair: '$arg'")

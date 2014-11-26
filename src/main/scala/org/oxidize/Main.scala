package org.oxidize

import java.io.File
import java.util.regex.Pattern
import javax.script.{ScriptContext, ScriptEngineManager, SimpleScriptContext}

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

@FunctionalInterface
trait MyFunction[T, R] {
  def apply(t: T): R
}

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
    val targetDir = if (args.length > 1) args(1) else "."

    applyTemplate(template, targetDir)
    0
  }

  def applyTemplate(templatePath: String, targetDir: String) {
    val template = FileUtils.readFileToString(new File(templatePath))
    val output = applyTemplate(template)
    val outFile = new File(targetDir, templatePath)
    FileUtils.writeStringToFile(outFile, output)
  }

  def applyTemplate(template: String): String = {
    val regex = Pattern.compile("@\\{=?([^@]+)\\}@")
    val matcher = regex.matcher(template)
    val sb = new StringBuffer
    val nashorn = new ScriptEngineManager().getEngineByName("nashorn")
    val context = new SimpleScriptContext()
    context.setAttribute("groupId", "com.example.js", ScriptContext.ENGINE_SCOPE)
    var customFileName: String = null
    val setFileName = new MyFunction[String, String] {
      def apply(str: String): String = {
        customFileName = str
        ""
      }

      override def toString: String = "SetFileName"
    }

    context.setAttribute("setFileName", setFileName, ScriptContext.ENGINE_SCOPE)
    while (matcher.find()) {
      val script = matcher.group(1)
      println("script to eval: " + script)
      val result = nashorn.eval(script, context)
      matcher.appendReplacement(sb, String.valueOf(result))
    }
    println("Custom file name is: '" + customFileName + "'")
    matcher.appendTail(sb)
    sb.toString
  }
}

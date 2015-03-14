package org.oxyjen

import java.io.File
import java.util.regex.Pattern
import javax.script.{ScriptException, ScriptContext, SimpleScriptContext, ScriptEngineManager}

import scala.util.Properties

object TemplateEngine {
  def applyTemplate(template: String, context: OxyjenContext): TemplateApplicationResult = {
    val regex = Pattern.compile("@\\{=?(.*?)\\}@", Pattern.DOTALL)
    val matcher = regex.matcher(template)
    val sb = new StringBuffer
    val nashorn = new ScriptEngineManager().getEngineByName("nashorn")
    val scriptContext = new SimpleScriptContext()
    for ((name, value) <- context.context) {
      bind(scriptContext, name, value)
    }
    val oxyjenJsObject = new OxyjenJsObject(context.targetFile)
    bind(scriptContext, "$o2", oxyjenJsObject)

    while (matcher.find()) {
      val expressionBlock = matcher.group(0).startsWith("@{=")
      val script = matcher.group(1)
      try {
        val result = nashorn.eval(script, scriptContext)
        matcher.appendReplacement(sb, if (expressionBlock) String.valueOf(result) else "")
      } catch {
        case e: ScriptException =>
          e match {
            case ReferenceError(name) =>
              return MissingValueInContext(name)
            case _ =>
              return ScriptExecutionFailure(e)
          }
      }
    }
    matcher.appendTail(sb)
    val rawOutput = sb.toString.trim
    val output = if (rawOutput.isEmpty) "" else rawOutput + Properties.lineSeparator
    SuccessfulApplication(output, oxyjenJsObject.fileName)
  }

  private def bind(scriptContext: SimpleScriptContext, name: String, value: Any) {
    scriptContext.setAttribute(name, value, ScriptContext.ENGINE_SCOPE)
  }

  private object ReferenceError {
    def unapply(e: ScriptException): Option[String] = {
      val msg = e.getMessage
      val regex = Pattern.compile("""ReferenceError: "([^"]*)" is not defined""")
      val matcher = regex.matcher(msg)
      if (matcher.find())
        Some(matcher.group(1))
      else
        None
    }
  }
}

class OxyjenJsObject(var fileName: String) {
  def setFileName(path: String): Unit = {
    println(s"$$o2.setFileName('$path') called")
    fileName = path
  }

  def setFileDir(dir: String): Unit = {
    val currentFile = new File(fileName)
    val newFile = new File(dir, currentFile.getName)
    fileName = newFile.getPath
  }
}

class OxyjenContext(val targetFile: String, val context: Map[String, Any])

sealed trait TemplateApplicationResult
case class SuccessfulApplication(output: String, targetFile: String)
  extends TemplateApplicationResult
case class MissingValueInContext(value: String) extends TemplateApplicationResult
case class ScriptExecutionFailure(e: ScriptException) extends TemplateApplicationResult

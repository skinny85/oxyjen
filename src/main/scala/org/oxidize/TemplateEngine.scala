package org.oxidize

import java.util.regex.Pattern
import javax.script.{ScriptContext, SimpleScriptContext, ScriptEngineManager}
import scala.util.Properties

object TemplateEngine {
  def applyTemplate(template: String, context: OxidizeContext): TemplateApplicationResult = {
    val regex = Pattern.compile("@\\{=?(.*?)\\}@", Pattern.DOTALL)
    val matcher = regex.matcher(template)
    val sb = new StringBuffer
    val nashorn = new ScriptEngineManager().getEngineByName("nashorn")
    val scriptContext = new SimpleScriptContext()
    for ((name, value) <- context.context) {
      bind(scriptContext, name, value)
    }
    val oxidizeJsObject = new OxidizeJsObject(context.targetFile)
    bind(scriptContext, "$oxidize", oxidizeJsObject)

    while (matcher.find()) {
      val script = matcher.group(1)
      val expressionBlock = matcher.group(0).startsWith("@{=")
      val result = nashorn.eval(script, scriptContext)
      matcher.appendReplacement(sb, if (expressionBlock) String.valueOf(result) else "")
    }
    matcher.appendTail(sb)
    val rawOutput = sb.toString.trim
    val output = if (rawOutput.isEmpty) "" else rawOutput + Properties.lineSeparator
    new TemplateApplicationResult(output, oxidizeJsObject.fileName)
  }

  private def bind(scriptContext: SimpleScriptContext, name: String, value: Any) {
    scriptContext.setAttribute(name, value, ScriptContext.ENGINE_SCOPE)
  }
}

class OxidizeJsObject(var fileName: String) {
  def setFileName(path: String): Unit = {
    println(s"$$oxidize.setFileName('$path') called")
    fileName = path
  }
}

class OxidizeContext(val targetFile: String, val context: Map[String, Any])

class TemplateApplicationResult(val output: String, val targetFile: String)

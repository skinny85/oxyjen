package org.oxyjen

import java.io.File
import java.util.regex.Pattern
import javax.script.{ScriptContext, SimpleScriptContext, ScriptEngineManager}
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
      val result = nashorn.eval(script, scriptContext)
      matcher.appendReplacement(sb, if (expressionBlock) String.valueOf(result) else "")
    }
    matcher.appendTail(sb)
    val rawOutput = sb.toString.trim
    val output = if (rawOutput.isEmpty) "" else rawOutput + Properties.lineSeparator
    new TemplateApplicationResult(output, oxyjenJsObject.fileName)
  }

  private def bind(scriptContext: SimpleScriptContext, name: String, value: Any) {
    scriptContext.setAttribute(name, value, ScriptContext.ENGINE_SCOPE)
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

class TemplateApplicationResult(val output: String, val targetFile: String)

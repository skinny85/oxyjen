package org.oxidize

import java.util.regex.Pattern
import javax.script.{ScriptContext, SimpleScriptContext, ScriptEngineManager}

object TemplateEngine {
  def applyTemplate(template: String, context: Map[String, Any]): String = {
    val regex = Pattern.compile("@\\{=?(.*?)\\}@", Pattern.DOTALL)
    val matcher = regex.matcher(template)
    val sb = new StringBuffer
    val nashorn = new ScriptEngineManager().getEngineByName("nashorn")
    val scriptContext = new SimpleScriptContext()
    for ((name, value) <- context) {
      bind(scriptContext, name, value)
    }
    bind(scriptContext, "$oxidize", new Oxidize)

    while (matcher.find()) {
      val script = matcher.group(1)
      val expressionBlock = matcher.group(0).startsWith("@{=")
      val result = nashorn.eval(script, scriptContext)
      matcher.appendReplacement(sb, if (expressionBlock) String.valueOf(result) else "")
    }
    matcher.appendTail(sb)
    sb.toString
  }

  private def bind(scriptContext: SimpleScriptContext, name: String, value: Any) {
    scriptContext.setAttribute(name, value, ScriptContext.ENGINE_SCOPE)
  }

  class Oxidize {
    def setFileName(path: String): Unit = {
      println(s"$$oxidize.setFileName('$path') called")
    }
  }
}

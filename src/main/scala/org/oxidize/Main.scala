package org.oxidize

import java.io.File
import java.util
import java.util.regex.Pattern

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
    val targetDir = if (args.length > 1) args(1) else "."

    val inString = FileUtils.readFileToString(new File(template))
    val context = new util.HashMap[String, String]()
    context.put("groupId", "com.example.yourcompany")
    val regex = Pattern.compile("@\\{=[\\s]*([^\\s]*)[\\s]*\\}@")
    val matcher = regex.matcher(inString)
    val sb = new StringBuffer
    while (matcher.find()) {
      matcher.appendReplacement(sb, context.get(matcher.group(1)))
    }
    matcher.appendTail(sb)
    val outFile = new File(targetDir, template)
    FileUtils.writeStringToFile(outFile, sb.toString)

    0
  }
}

package org.oxyjen

import org.oxyjen.common.{ReturnCode, StdIo}

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

    val (targetDir, params) = parseTargetDirAndParams(args.tail)
    TemplateDriver.applyTemplateExtractingItIfNeccessary(args.head, targetDir, params) {
      ReturnCode.Success
    }
  }

  def parseTargetDirAndParams(args: Seq[String]): (String, Seq[String]) = {
    def looksLikeTargetDir(arg: String) = !arg.contains('=')

    if (args.isEmpty) {
      (".", Seq.empty[String])
    } else if (looksLikeTargetDir(args.head)) {
      (args.head, args.tail)
    } else {
      (".", args)
    }
  }
}

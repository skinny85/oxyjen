package org.oxyjen.ozone.commands

import java.io.File

import org.oxyjen.TemplateDriver
import org.oxyjen.common.{FileHelper, ReturnCode, StdIo}

object Test {
  def main(args: String*): ReturnCode = {
    if (args.length < 2) {
      StdIo pute "Usage: ozone test TEMPLATE REFERENCE_DIRECTORY [NAME=VALUE]... "
      return ReturnCode.IncorrectNumberOfArguments
    }

    val tmpDir = FileHelper.transientTempDir("oxyjen_results")
    val params = args.slice(2, args.length)
    TemplateDriver.applyTemplateExtractingItIfNeccessary(args.head, tmpDir.getPath, params) {
      dirIsInside(tmpDir.getPath, args(1))
    }
  }

  private def dirIsInside(result: String, reference: String): ReturnCode = {
    def d2d(first: File, second: File): Option[File] = {
      if (first.isDirectory) {
        for (file <- first.listFiles()) {
          val ret = d2d(file, second)
          if (ret.isDefined)
            return ret
        }
      } else {
        if (!new File(second, first.getName).exists())
          return Some(first)
      }
      None
    }

    d2d(new File(result), new File(reference)) match {
      case Some(missing) =>
        StdIo.pute("File '{}' expected but missing", missing.getPath)
        ReturnCode.ContradictoryArguments // TODO fix ret code
      case None =>
        StdIo puts "All tests pass"
        ReturnCode.Success
    }
  }
}

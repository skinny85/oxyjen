package org.oxyjen.common

import java.io.File
import java.nio.file.Files

import org.apache.commons.io.FileUtils

object FileHelper {
  def transientTempDir(prefix: String): File = {
    val ret = tempDir(prefix)
    deleteOnExit(ret)
    ret
  }

  def tempDir(prefix: String): File = {
    Files.createTempDirectory(prefix).toFile
  }

  def deleteOnExit(tempDir: File) {
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
      def run(): Unit = {
        FileUtils.deleteDirectory(tempDir)
      }
    }))
  }
}

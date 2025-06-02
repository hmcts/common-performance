package stats

import java.io.File

object StatsGenerator {

  // An outputBasePath is optional, and shouldn't be used by Gatling tests, only for scala tests
  def run(statsFile: File, transactionNamesToGraph: Set[String], outputBasePath: Option[String] = None): Unit = {
    if (!statsFile.exists()) {
      throw new Exception(s"File not found: ${statsFile.getAbsolutePath}")
    }

    // using endsWith to allow scalaTest test files to work
    if (statsFile.getName.endsWith("stats.json")) {
      JsonStatsProcessor.processJsonStats(statsFile, transactionNamesToGraph, outputBasePath)
    } else if (statsFile.getName.endsWith("index.html")) {
      HtmlStatsProcessor.processHtmlStats(statsFile, transactionNamesToGraph, outputBasePath)
    } else {
      throw new Exception(s"Unsupported file type: ${statsFile.getAbsolutePath}")
    }
  }
}

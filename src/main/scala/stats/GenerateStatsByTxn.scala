package stats

import java.io.File

/*
THIS CODE EXTRACTS THE STATS.JSON FROM THE TEST AND CALLS THE STATS GENERATOR TO CREATE STATS PER GATLING TRANSACTION
THIS WILL REPLACE THE AGGREGATED METRICS USED BY THE GATLING JENKINS PLUGIN WITH INDIVIDUAL METRICS PER TRANSACTION
 */

object GenerateStatsByTxn {
  def main(args: Array[String]): Unit = {

    val transactionNamesToGraph = args.toSet

    // Get the base directory for Gatling reports
    val baseDir = new File("build/reports/gatling/")

    // Check if the base directory exists
    if (!baseDir.exists()) {
      println(s"The base directory does not exist: $baseDir")
      return
    }

    // List all directories in the base directory
    val subDirs = baseDir.listFiles.filter(_.isDirectory)

    // If no subdirectories exist, exit
    if (subDirs.isEmpty) {
      println(s"No subdirectories found under: $baseDir")
      return
    }

    // Sort directories by last modified date to get the latest one
    val latestDir = subDirs.maxBy(_.lastModified)

    // Construct the path to stats.json or index.html (based on the version of Gatling being used) within the latest directory
    val statsJsonPath = new File(latestDir, "js/stats.json")
    val indexHtmlPath = new File(latestDir, "index.html")

    val statsFile =
      if (statsJsonPath.exists()) {
        if (transactionNamesToGraph.isEmpty) {
          println("No transaction names provided. Not running the stats generation process for stats.json input.")
          return
        }
        println(s"Detected legacy JSON input. Using stats.json from: ${statsJsonPath.getAbsolutePath}")
        statsJsonPath
      } else if (indexHtmlPath.exists()) {
        println(s"stats.json not found, but HTML report has been detected. Falling back to index.html at: ${indexHtmlPath.getAbsolutePath}")
        indexHtmlPath
      } else {
        println(s"Neither stats.json nor index.html found in: $latestDir")
        return
      }

    println(s"Successfully loaded input from: ${statsFile.getAbsolutePath}")

    StatsGenerator.run(statsFile, transactionNamesToGraph)
  }
}

package stats

import com.google.gson.{JsonElement, JsonObject, JsonParser}
import java.io.{File, FileReader}
import scala.jdk.CollectionConverters._

object JsonStatsProcessor {

  def processJsonStats(jsonFile: File, transactionNamesToGraph: Set[String], outputBasePath: Option[String]): Unit = {

    println(s"[JsonStatsProcessor] Reading stats.json from: ${jsonFile.getAbsolutePath}")

    // Determine the output location (if it's a Gatling test or a scalaTest):
    val outputPath = outputBasePath match {
      case Some(base) => s"$base"
      case None => "build/reports/gatling"
    }

    val jsonElement: JsonElement = try {
      JsonParser.parseReader(new FileReader(jsonFile))
    } catch {
      case e: Exception =>
        println(s"[JsonStatsProcessor] Failed to parse stats.json: ${e.getMessage}")
        return
    }

    if (!jsonElement.isJsonObject || !jsonElement.getAsJsonObject.has("contents")) {
      throw new Exception(s"Invalid stats.json format")
    }

    if (transactionNamesToGraph.nonEmpty) {
      println(s"[JsonStatsProcessor] Generating stats for the following transactions: ${transactionNamesToGraph.mkString(", ")}")
    } else {
      println("[JsonStatsProcessor] No transactions specified. No stats will be generated.")
      return
    }

    val contents = jsonElement.getAsJsonObject.getAsJsonObject("contents")

    for (entry <- contents.entrySet().asScala) {
      val reqElement = entry.getValue

      val request = reqElement.getAsJsonObject
      val stats = request.getAsJsonObject("stats")
      val name = sanitizeName(stats.get("name").getAsString)

      if (transactionNamesToGraph.contains(name)) {
        val numberOfRequests = getStatsForMetric(stats, "numberOfRequests")
        val minResponseTime = getStatsForMetric(stats, "minResponseTime")
        val maxResponseTime = getStatsForMetric(stats, "maxResponseTime")
        val meanResponseTime = getStatsForMetric(stats, "meanResponseTime")
        val standardDeviation = getStatsForMetric(stats, "standardDeviation")
        val percentiles1 = getStatsForMetric(stats, "percentiles1")
        val percentiles2 = getStatsForMetric(stats, "percentiles2")
        val percentiles3 = getStatsForMetric(stats, "percentiles3")
        val percentiles4 = getStatsForMetric(stats, "percentiles4")
        val group1 = getStatsForGroup(stats, "group1")
        val group2 = getStatsForGroup(stats, "group2")
        val group3 = getStatsForGroup(stats, "group3")
        val group4 = getStatsForGroup(stats, "group4")
        val meanNumberOfRequestsPerSecond = getStatsForMetric(stats, "meanNumberOfRequestsPerSecond")

        StatsWriter.createFakeSimulation(
          name, numberOfRequests, minResponseTime, maxResponseTime, meanResponseTime, standardDeviation,
          percentiles1, percentiles2, percentiles3, percentiles4, group1, group2, group3, group4,
          meanNumberOfRequestsPerSecond, s"${outputPath}/${name}-simulation-txnStats/js"
        )
      }
    }
  }

  private def sanitizeName(name: String): String = {
    name.replaceAll("[^a-zA-Z0-9-_]", "_")
  }

  private def getStat(stats: JsonObject, statKey: String, subKey: String): Double = {
    if (stats.has(statKey) && stats.getAsJsonObject(statKey).has(subKey)) {
      stats.getAsJsonObject(statKey).get(subKey).getAsDouble
    } else {
      println(s"[JsonStatsProcessor] Missing '$statKey' or '$subKey' for request")
      0.0
    }
  }

  private def getStatsForMetric(stats: JsonObject, metric: String): MetricValues = {
    val total = getStat(stats, metric, "total")
    val ok = getStat(stats, metric, "ok")
    val ko = getStat(stats, metric, "ko")
    MetricValues(total, ok, ko)
  }

  private def getStatsForGroup(stats: JsonObject, group: String): GroupValues = {
    val count = getStat(stats, group, "count").toInt
    val percentage = getStat(stats, group, "percentage")
    GroupValues(count, percentage)
  }
}

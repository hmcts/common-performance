package stats

import java.io.File
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import scala.jdk.CollectionConverters._

object HtmlStatsProcessor {

  // Mappings to the column number in the HTML e.g. <th id="col-7" class="header sortable"><span>Min</span></th>
  // Column 5 (% KO) is not reported in the global_stats.json, so has been omitted.
  private val htmlMetricColumns = Map(
    "numberOfRequests-total" -> 2,
    "numberOfRequests-ok" -> 3,
    "numberOfRequests-ko" -> 4,
    "meanNumberOfRequestsPerSecond" -> 6,
    "minResponseTime" -> 7,
    "percentiles1" -> 8, // 50th percentile
    "percentiles2" -> 9, // 75th percentile
    "percentiles3" -> 10, // 95th percentile
    "percentiles4" -> 11, // 99th percentile
    "maxResponseTime" -> 12,
    "meanResponseTime" -> 13,
    "standardDeviation" -> 14
  )

  def processHtmlStats(htmlFile: File, transactionNamesToGraph: Set[String], outputBasePath: Option[String]): Unit = {

    println(s"[HtmlStatsProcessor] Reading index.html from: ${htmlFile.getAbsolutePath}")

    val transactionsMap = extractTransactionMetricsFromHtml(htmlFile)
    val simulationSummary = extractSummaryStatsFromHtml(htmlFile)

    // Determine the output locations (if it's a Gatling test or a scalaTest):
    val summaryOutputPath = outputBasePath match {
      case Some(base) => s"$base/js"
      case None => new File(htmlFile.getParentFile, "js").getAbsolutePath
    }
    val transactionOutputPath = outputBasePath match {
      case Some(base) => s"$base"
      case None => "build/reports/gatling"
    }

    simulationSummary.foreach { summary =>
      val name = "AllRequests"
      val (g1, g2, g3, g4) = extractGroupValuesFromHtml(htmlFile)

      val (
        numberOfRequests, minResponseTime, maxResponseTime, meanResponseTime, standardDeviation,
        percentiles1, percentiles2, percentiles3, percentiles4, group1, group2, group3, group4, meanNumberOfRequestsPerSecond
        ) = buildMetrics(summary, (g1, g2, g3, g4))

      StatsWriter.createFakeSimulation(
        name, numberOfRequests, minResponseTime, maxResponseTime, meanResponseTime, standardDeviation,
        percentiles1, percentiles2, percentiles3, percentiles4, group1, group2, group3, group4, meanNumberOfRequestsPerSecond,
        summaryOutputPath
      )
    }

    transactionsMap.foreach { case (name, metrics) =>
      if (transactionNamesToGraph.contains(name)) {

        // Group stats are only available at the summary level in index.html, so populate the transaction level with zeros
        val g1 = GroupValues(0, 0.0)
        val g2 = GroupValues(0, 0.0)
        val g3 = GroupValues(0, 0.0)
        val g4 = GroupValues(0, 0.0)

        val (
          numberOfRequests, minResponseTime, maxResponseTime, meanResponseTime, standardDeviation,
          percentiles1, percentiles2, percentiles3, percentiles4, group1, group2, group3, group4, meanNumberOfRequestsPerSecond
          ) = buildMetrics(metrics, (g1, g2, g3, g4))

        StatsWriter.createFakeSimulation(
          name, numberOfRequests, minResponseTime, maxResponseTime, meanResponseTime, standardDeviation,
          percentiles1, percentiles2, percentiles3, percentiles4, group1, group2, group3, group4, meanNumberOfRequestsPerSecond,
          s"${transactionOutputPath}/${name}-simulation-txnStats/js"
        )
      }
    }
  }

  def extractSummaryStatsFromHtml(htmlFile: File): Option[Map[String, String]] = {
    val doc: Document = Jsoup.parse(htmlFile, "UTF-8")

    val row = doc.select("#statistics_table_container table tbody tr").asScala
      .find(_.select(".ellipsed-name").text().trim == "All Requests")

    row.map { tr =>
      val columns = tr.select("td").asScala
      htmlMetricColumns.map { case (metric, idx) =>
        val raw = if ((idx -1) < columns.size) columns(idx -1).text().trim else "0"
        val value = if (raw == "-") "0" else raw
        metric -> value
      }
    }
  }

  def extractTransactionMetricsFromHtml(htmlFile: File): Map[String, Map[String, String]] = {
    val doc: Document = Jsoup.parse(htmlFile, "UTF-8")
    val tableRows = doc.select("#statistics_table_container table tbody tr")

    tableRows.asScala.flatMap { row =>
      val columns = row.select("td").asScala
      if (columns.isEmpty) None
      else {
        val nameSpan = row.select(".ellipsed-name").text().trim
        if (nameSpan.nonEmpty && nameSpan != "All Requests") {
          val metrics = htmlMetricColumns.map { case (metric, idx) =>
            val raw = if ((idx - 1) < columns.size) columns(idx - 1).text().trim else "0"
            val value = if (raw == "-") "0" else raw
            metric -> value
          }.toMap
          Some(nameSpan -> metrics)
        } else None
      }
    }.toMap
  }

  def extractGroupValuesFromHtml(htmlFile: File): (GroupValues, GroupValues, GroupValues, GroupValues) = {
    val doc = Jsoup.parse(htmlFile, "UTF-8")
    val scripts = doc.select("script").asScala

    // Find the Highcharts script block for "Response Time Ranges"
    val targetScript = scripts.find(_.html().contains("Response Time Ranges"))

    val defaultGroups = (GroupValues(0, 0.0), GroupValues(0, 0.0), GroupValues(0, 0.0), GroupValues(0, 0.0))

    targetScript match {
      case Some(script) =>
        val scriptText = script.html()

        // Match the column `data: [{ color: ..., y: <count> }, ...]`
        val columnPattern = """type:\s*'column',\s*data:\s*\[([\s\S]*?)\]""".r
        val columnDataStr = columnPattern.findFirstMatchIn(scriptText).map(_.group(1)).getOrElse("")
        val countPattern = """y:\s*(\d+)""".r
        val counts = countPattern.findAllMatchIn(columnDataStr).map(_.group(1).toInt).toList

        // Match the pie chart `data: [{ name: ..., y: <percentage> }, ...]`
        val piePattern = """type:\s*'pie',[\s\S]*?data:\s*\[([\s\S]*?)\]""".r
        val pieDataStr = piePattern.findFirstMatchIn(scriptText).map(_.group(1)).getOrElse("")
        val percentPattern = """y:\s*([\d.]+)""".r
        val percentages = percentPattern.findAllMatchIn(pieDataStr).map(_.group(1).toDouble).toList

        if (counts.size == 4 && percentages.size == 4) {
          (
            GroupValues(counts(0), percentages(0)),
            GroupValues(counts(1), percentages(1)),
            GroupValues(counts(2), percentages(2)),
            GroupValues(counts(3), percentages(3))
          )
        } else {
          println("[HtmlStatsProcessor] Could not extract exactly 4 groups from Highcharts block.")
          defaultGroups
        }

      case None =>
        println("[HtmlStatsProcessor] 'Response Time Ranges' chart not found in HTML.")
        defaultGroups
    }
  }

  private def buildMetrics(metrics: Map[String, String], groups: (GroupValues, GroupValues, GroupValues, GroupValues)): (
    MetricValues, MetricValues, MetricValues, MetricValues, MetricValues,
      MetricValues, MetricValues, MetricValues, MetricValues,
      GroupValues, GroupValues, GroupValues, GroupValues,
      MetricValues
    ) = {

    def getDouble(key: String): Double = metrics.getOrElse(key, "0").replace("-", "0").toDouble

    val numberOfRequests = MetricValues(getDouble("numberOfRequests-total"), getDouble("numberOfRequests-ok"), getDouble("numberOfRequests-ko"))
    val minResponseTime = MetricValues(getDouble("minResponseTime"), getDouble("minResponseTime"), 0.0)
    val maxResponseTime = MetricValues(getDouble("maxResponseTime"), getDouble("maxResponseTime"), 0.0)
    val meanResponseTime = MetricValues(getDouble("meanResponseTime"), getDouble("meanResponseTime"), 0.0)
    val standardDeviation = MetricValues(getDouble("standardDeviation"), getDouble("standardDeviation"), 0.0)
    val percentiles1 = MetricValues(getDouble("percentiles1"), getDouble("percentiles1"), 0.0)
    val percentiles2 = MetricValues(getDouble("percentiles2"), getDouble("percentiles2"), 0.0)
    val percentiles3 = MetricValues(getDouble("percentiles3"), getDouble("percentiles3"), 0.0)
    val percentiles4 = MetricValues(getDouble("percentiles4"), getDouble("percentiles4"), 0.0)
    val meanNumberOfRequestsPerSecond = MetricValues(getDouble("meanNumberOfRequestsPerSecond"), getDouble("meanNumberOfRequestsPerSecond"), 0.0)

    val (group1, group2, group3, group4) = groups

    (
      numberOfRequests, minResponseTime, maxResponseTime, meanResponseTime, standardDeviation,
      percentiles1, percentiles2, percentiles3, percentiles4, group1, group2, group3, group4, meanNumberOfRequestsPerSecond
    )
  }

}

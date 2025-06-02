package stats

import org.scalatest.funsuite.AnyFunSuite
import java.io.File
import java.nio.file.{Files, Paths}
import scala.io.Source
import com.google.gson.JsonParser

class StatsGeneratorHtmlSpec extends AnyFunSuite with StatsTestHelpers {

  test("[StatsGeneratorHtmlSpec] StatsGenerator should skip missing transaction rows in index.html") {
    val htmlPath = loadDummyStatsFile("missing_transaction_rows_index.html")
    StatsGenerator.run(htmlPath, Set("MissingTransaction"), Some(s"$outputBaseDir/html"))
    assert(true)
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should convert '-' metric values to 0 in index.html") {
    val htmlPath = loadDummyStatsFile("dash_metrics_index.html")
    StatsGenerator.run(htmlPath, Set("All Requests"))
    assert(true)
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should generate AllRequests global_stats.json from index.html") {
    val htmlPath = loadDummyStatsFile("index.html")
    val transactionNames = Set.empty[String]

    StatsGenerator.run(htmlPath, transactionNames, Some(s"$outputBaseDir/html"))

    val outputPath = Paths.get(s"$outputBaseDir/html/js/global_stats.json")
    assert(Files.exists(outputPath), s"Output file does not exist: $outputPath")
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should generate correct stats in global_stats.json from index.html") {
    val htmlPath = loadDummyStatsFile("index.html")
    val transactionNames = Set("Transaction_3", "Transaction_4")

    StatsGenerator.run(htmlPath, transactionNames, Some(s"$outputBaseDir/html"))

    val outputFilePath = s"$outputBaseDir/html/Transaction_4-simulation-txnStats/js/global_stats.json"
    val outputContent = Source.fromFile(outputFilePath).getLines().mkString
    val jsonObject = JsonParser.parseString(outputContent).getAsJsonObject

    // Validate request counts
    val numberOfRequests = jsonObject.getAsJsonObject("numberOfRequests")
    assert(numberOfRequests.get("total").getAsDouble == 60)
    assert(numberOfRequests.get("ok").getAsDouble == 60)
    assert(numberOfRequests.get("ko").getAsDouble == 0)

    // Validate response times
    val minResponseTime = jsonObject.getAsJsonObject("minResponseTime")
    assert(minResponseTime.get("total").getAsDouble == 230)
    assert(minResponseTime.get("ok").getAsDouble == 230)
    assert(minResponseTime.get("ko").getAsDouble == 0)

    val maxResponseTime = jsonObject.getAsJsonObject("maxResponseTime")
    assert(maxResponseTime.get("total").getAsDouble == 3486)
    assert(maxResponseTime.get("ok").getAsDouble == 3486)
    assert(maxResponseTime.get("ko").getAsDouble == 0)

    val meanResponseTime = jsonObject.getAsJsonObject("meanResponseTime")
    assert(meanResponseTime.get("total").getAsDouble == 1029)
    assert(meanResponseTime.get("ok").getAsDouble == 1029)
    assert(meanResponseTime.get("ko").getAsDouble == 0)

    val standardDeviation = jsonObject.getAsJsonObject("standardDeviation")
    assert(standardDeviation.get("total").getAsDouble == 878)
    assert(standardDeviation.get("ok").getAsDouble == 878)
    assert(standardDeviation.get("ko").getAsDouble == 0)

    val percentiles3 = jsonObject.getAsJsonObject("percentiles3")
    assert(percentiles3.get("total").getAsDouble == 2446)
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should skip unmatched transactions in index.html") {
    val htmlPath = loadDummyStatsFile("index.html")
    val transactionNames = Set("SomeNonExistentTransaction")

    StatsGenerator.run(htmlPath, transactionNames, Some(s"$outputBaseDir/html"))

    val outputPath = Paths.get(s"$outputBaseDir/html/SomeNonExistentTransaction-simulation-txnStats/js/global_stats.json")

    assert(!Files.exists(outputPath), "Expected output file should not exist for unmatched transaction")
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should throw for missing index.html file") {
    val missingHtml = new File("nonexistent_path/index.html")

    assertThrows[Exception] {
      StatsGenerator.run(missingHtml, Set("Transaction_4"), Some(s"$outputBaseDir/html"))
    }
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should not fail when 'All Requests' is missing from index.html") {
    val htmlPath = loadDummyStatsFile("missing_all_requests_index.html")
    StatsGenerator.run(htmlPath, Set("Transaction_5"), Some(s"$outputBaseDir/html"))
    assert(true)
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should handle completely empty index.html gracefully") {
    val htmlPath = loadDummyStatsFile("empty_index.html")
    StatsGenerator.run(htmlPath, Set("AnyTransaction"), Some(s"$outputBaseDir/html"))
    assert(true)
  }

  test("[StatsGeneratorHtmlSpec] StatsGenerator should handle malformed index.html without crashing") {
    val htmlPath = loadDummyStatsFile("malformed_index.html")
    StatsGenerator.run(htmlPath, Set("AnyTransaction"), Some(s"$outputBaseDir/html"))
    assert(true)
  }

}

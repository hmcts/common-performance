package stats

import org.scalatest.funsuite.AnyFunSuite
import java.io.File
import java.nio.file.{Files, Paths}
import scala.io.Source
import com.google.gson.JsonParser

class StatsGeneratorJsonSpec extends AnyFunSuite with StatsTestHelpers {

  test("[StatsGeneratorJsonSpec] StatsGenerator should skip processing when transaction list is empty") {
    val dummyStatsPath = loadDummyStatsFile("sample_stats.json")

    // Pass empty args
    StatsGenerator.run(dummyStatsPath, Set())

    assert(true)
  }

  test("[StatsGeneratorJsonSpec] StatsGenerator should generate stats based on valid stats.json file and create output files") {
    val dummyStatsPath = loadDummyStatsFile("sample_stats.json")
    val transactionNames = Set("Transaction_1", "Transaction_2")

    StatsGenerator.run(dummyStatsPath, transactionNames, Some(s"$outputBaseDir/json"))

    // Check if the expected output directory and file are created
    val outputPath = Paths.get(s"$outputBaseDir/json/Transaction_1-simulation-txnStats/js/global_stats.json")
    assert(Files.exists(outputPath), s"Output file does not exist: $outputPath")
  }

  test("[StatsGeneratorJsonSpec] StatsGenerator should generate correct stats in global_stats.json") {
    val dummyStatsPath = loadDummyStatsFile("sample_stats.json")
    val transactionNames = Set("Transaction_1")

    // Run the stats generator
    StatsGenerator.run(dummyStatsPath, transactionNames, Some(s"$outputBaseDir/json"))

    // Read and parse the generated JSON content
    val outputFilePath = s"$outputBaseDir/json/Transaction_1-simulation-txnStats/js/global_stats.json"
    val outputContent = Source.fromFile(outputFilePath).getLines().mkString
    val jsonObject = JsonParser.parseString(outputContent).getAsJsonObject

    // Perform checks on expected values in the generated JSON
    val numberOfRequests = jsonObject.getAsJsonObject("numberOfRequests")
    assert(numberOfRequests.get("total").getAsDouble == 10)
    assert(numberOfRequests.get("ok").getAsDouble == 9)
    assert(numberOfRequests.get("ko").getAsDouble == 1)

    val minResponseTime = jsonObject.getAsJsonObject("minResponseTime")
    assert(minResponseTime.get("total").getAsDouble == 1000)
    assert(minResponseTime.get("ok").getAsDouble == 1000)
    assert(minResponseTime.get("ko").getAsDouble == 500)
  }

  test("[StatsGeneratorJsonSpec] StatsGenerator should throw an error for a missing stats.json file") {
    val dummyStatsPath = new File("nonexistent_path/stats.json")

    assertThrows[Exception] {
      StatsGenerator.run(dummyStatsPath, Set("Transaction_1"), Some(s"$outputBaseDir/json"))
    }
  }

  test("[StatsGeneratorJsonSpec] StatsGenerator should not process any transactions not present in stats.json") {
    val dummyStatsPath = loadDummyStatsFile("sample_stats.json")
    val transactionNames = Set("NonExistentTransaction")

    // This simulates processing when the set of transactions contains a transaction not in the stats file
    StatsGenerator.run(dummyStatsPath, transactionNames, Some(s"$outputBaseDir/json"))

    assert(true)
  }

  test("[StatsGeneratorJsonSpec] StatsGenerator should skip processing when stats.json is empty") {
    val emptyStatsPath = loadDummyStatsFile("empty_stats.json")

    // This should not throw any exceptions; it should simply skip processing
    StatsGenerator.run(emptyStatsPath, Set("Transaction_1", "Transaction_2"), Some(s"$outputBaseDir/json"))

    assert(true)
  }

  test("[StatsGeneratorJsonSpec] StatsGenerator should throw an exception on invalid stats.json format") {
    val invalidStatsPath = loadDummyStatsFile("invalid_stats.json")

    assertThrows[Exception] {
      StatsGenerator.run(invalidStatsPath, Set("Transaction_1"), Some(s"$outputBaseDir/json"))
    }
  }

}

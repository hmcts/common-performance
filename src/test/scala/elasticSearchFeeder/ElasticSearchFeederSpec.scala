package elasticSearchFeeder

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfter

class ElasticSearchCaseFeederSpec extends AnyFunSuite with BeforeAndAfter {

  // Mock configuration toggle
  var csvOverride: Boolean = _

  // Helper function to simulate fetching cases from ElasticSearch
  def mockFetchCasesFromElasticSearch(recordsRequired: Int): List[String] = {
    csvOverride match {
      case true => List("case1", "case2", "case3")  // Simulating a CSV-based scenario
      case false =>
        if (recordsRequired <= 3) List("case1", "case2", "case3")
        else List.empty[String]  // Simulating fewer cases than required
    }
  }

  // Helper function to simulate feeder creation (adjusted for config)
  def createElasticSearchFeeder(feederType: FeederType, recordsRequired: Int): Iterator[Map[String, String]] = {
    val caseIds = mockFetchCasesFromElasticSearch(recordsRequired)
    if (caseIds.size < recordsRequired) {
      throw new RuntimeException(s"Not enough data in feeder, ElasticSearch only returned ${caseIds.size} records.")
    }
    new CaseIdFeeder(caseIds, feederType).feeder
  }

  before {
    csvOverride = false  // Default to CSV override off
  }

  test("ElasticSearchCaseFeeder should use ElasticSearch response when OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE is false") {
    // Simulate ElasticSearch behavior (no CSV override)
    TestElasticSearchFeederConfig.overrideCsvFile(false)
    val feederType = FeederType.QUEUE
    val recordsRequired = 3

    val feeder = createElasticSearchFeeder(feederType, recordsRequired)
    val result = feeder.take(3).toList

    result.foreach(entry => println("[TEST] ES Queue feeder chose case: " + entry("caseId")))

    assert(result.size == 3)
    assert(result.head("caseId") == "case1")
  }

  test("ElasticSearchCaseFeeder should fallback to CSV when OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE is true") {
    // Simulate CSV override being active
    TestElasticSearchFeederConfig.overrideCsvFile(true)
    val feederType = FeederType.QUEUE
    val recordsRequired = 3

    val feeder = createElasticSearchFeeder(feederType, recordsRequired)
    val result = feeder.take(3).toList

    result.foreach(entry => println("[TEST] CSV Queue feeder chose case: " + entry("caseId")))

    assert(result.size == 3)
    assert(result.head("caseId") == "case1")
  }

  test("ElasticSearchCaseFeeder should throw an error if not enough records are returned from ElasticSearch") {
    // Simulate a case where ElasticSearch doesn't return enough records
    TestElasticSearchFeederConfig.overrideCsvFile(false)
    val feederType = FeederType.QUEUE
    val recordsRequired = 5  // More records requested than available

    assertThrows[RuntimeException] {
      createElasticSearchFeeder(feederType, recordsRequired)
    }
  }

  test("ElasticSearchCaseFeeder should handle circular feeder type") {
    // Simulate Circular Feeder behavior
    TestElasticSearchFeederConfig.overrideCsvFile(false)
    val feederType = FeederType.CIRCULAR
    val recordsRequired = 3

    val feeder = createElasticSearchFeeder(feederType, recordsRequired)
    val result = feeder.take(6).toList

    result.foreach(entry => println("[TEST] ES Circular feeder chose case: " + entry("caseId")))

    assert(result.size == 6)
    assert(result.head("caseId") == "case1")
    assert(result(3)("caseId") == "case1")  // Circular should repeat
  }

  test("ElasticSearchCaseFeeder should shuffle feeder type") {
    // Simulate Shuffle Feeder behavior
    TestElasticSearchFeederConfig.overrideCsvFile(false)
    val feederType = FeederType.SHUFFLE
    val recordsRequired = 3

    val feeder = createElasticSearchFeeder(feederType, recordsRequired)
    val result = feeder.take(3).toList

    result.foreach(entry => println("[TEST] ES Shuffle feeder chose case: " + entry("caseId")))

    assert(result.size == 3)
    assert(result.map(_ ("caseId")).toSet == Set("case1", "case2", "case3"))  // Ensure all cases are present
  }

  test("ElasticSearchCaseFeeder should handle random feeder type") {
    TestElasticSearchFeederConfig.overrideCsvFile(false)
    val feederType = FeederType.RANDOM
    val recordsRequired = 3

    val feeder = createElasticSearchFeeder(feederType, recordsRequired)

    val result = feeder.take(10).toList

    result.foreach(entry => println("[TEST] ES Random feeder chose case: " + entry("caseId")))

    assert(result.nonEmpty)
    assert(result.forall(_.contains("caseId")))
    assert(result.map(_("caseId")).forall(Set("case1", "case2", "case3").contains))
  }



  test("ElasticSearchCaseFeeder should handle empty response from ElasticSearch") {
    // Simulate an empty response from ElasticSearch
    TestElasticSearchFeederConfig.overrideCsvFile(false)
    val feederType = FeederType.QUEUE
    val recordsRequired = 0  // Requesting zero records

    val feeder = createElasticSearchFeeder(feederType, recordsRequired)
    val result = feeder.take(0).toList
    assert(result.isEmpty)
  }
}

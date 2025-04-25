package elasticSearchFeeder

import io.gatling.core.feeder.Feeder
import io.gatling.core.Predef._

object ElasticSearchCaseFeeder {

  def feeder(feederType: FeederType, recordsRequired: Int): Feeder[String] = {

    if (ElasticSearchFeederConfig.OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE) {
      println("INFO: CSV file override enabled, using CSV feeder.")
      csv("caseIds.csv").circular().asInstanceOf[Feeder[String]]
    }
    else {
      println("INFO: Creating ElasticSearch feeder...")
      val caseIds = FetchCasesFromElasticSearch.fetchCaseIds(recordsRequired)

      if (caseIds.size < recordsRequired) {
        println(s"ERROR: Only ${caseIds.size} records fetched, but $recordsRequired required.")
        sys.exit(1)
      }

      println(s"INFO: ${caseIds.size} cases retrieved. Creating a $feederType feeder...")
      val caseIdFeeder = new CaseIdFeeder(caseIds, feederType)
      caseIdFeeder.feeder
    }
  }
}

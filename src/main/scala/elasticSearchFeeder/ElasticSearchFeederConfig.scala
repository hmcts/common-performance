package elasticSearchFeeder

case class ElasticSearchFeederConfigDefaultValues(
  ELASTICSEARCH_SERVER_DIRECT: String = "ccd-elastic-search-perftest.service.core-compute-perftest.internal",
  ELASTICSEARCH_SERVER_LOCAL: String = "localhost",
  ELASTICSEARCH_SERVER_PORT: String = "9200",
  hostnameStringsWithDirectElasticSearchConnectivity: List[String] = List("mgmt-perf-test-vm", "cnp-jenkins"),
  ELASTICSEARCH_INDEX: String = "et_englandwales_cases-000001",
  ELASTICSEARCH_QUERY_PATH: String = "src/gatling/resources/elasticSearchQuery.json",
  RECORDS_REQUIRED_OVERRIDE: Int = -1,
  OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE: Boolean = false
)

object ElasticSearchFeederConfig {
  private var activeConfig: ElasticSearchFeederConfigDefaultValues = ElasticSearchFeederConfigDefaultValues()

  def config: ElasticSearchFeederConfigDefaultValues = activeConfig

  def set(overrideConfig: ElasticSearchFeederConfigDefaultValues): Unit = {
    activeConfig = overrideConfig
  }
}

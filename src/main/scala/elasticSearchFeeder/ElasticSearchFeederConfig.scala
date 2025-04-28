package elasticSearchFeeder

case class ElasticSearchFeederConfigDefaultValues(
  ELASTICSEARCH_SERVER_DIRECT: String = "ccd-elastic-search-perftest.service.core-compute-perftest.internal",
  ELASTICSEARCH_SERVER_LOCAL: String = "localhost",
  ELASTICSEARCH_SERVER_PORT: String = "9200",
  hostnameStringsWithDirectElasticSearchConnectivity: List[String] = List("mgmt-perf-test-vm", "cnp-jenkins"),
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

object esIndices {

  val Adoption = "a58_cases-000001"
  val Benefit = "benefit_cases-000001"
  val Civil = "civil_cases-000001"
  val ET_EnglandWales = "et_englandwales_cases-000001"
  val ET_Scotland = "et_scotland_cases-000001"
  val FPL = "care_supervision_epo_cases-000001"
  val FR = "financialremedymvp2_cases-000001"
  val IA_Asylum = "asylum_cases-000001"
  val IA_Bail = "bail_cases-000001"
  val NFD = "nfd_cases-000001"
  val PRL = "prlapps_cases-000001"
  val Probate_Caveat = "caveat_cases-000001"
  val Probate_GoR = "grantofrepresentation_cases-000001"

}

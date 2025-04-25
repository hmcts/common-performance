package elasticSearchFeeder

object ElasticSearchFeederConfig {

  val ELASTICSEARCH_SERVER_DIRECT = "ccd-elastic-search-perftest.service.core-compute-perftest.internal"
  val ELASTICSEARCH_SERVER_LOCAL = "localhost"
  val ELASTICSEARCH_SERVER_PORT = "9200"

  // List of substrings of server hostnames that need to run this simulation,
  // who have a direct connection to the ElasticSearch perftest instance (such as VMs, Jenkins, etc)
  val hostnameStringsWithDirectElasticSearchConnectivity = List("mgmt-perf-test-vm", "cnp-jenkins")

  val ELASTICSEARCH_INDEX = "et_englandwales_cases-000001"
  val ELASTICSEARCH_QUERY_PATH = "src/gatling/resources/bodies/elasticSearchQuery.json"

  // ONLY SUPPLY A NUMBER BELOW IF YOU NEED A CUSTOM NUMBER OF CASE IDs FROM ELASTICSEARCH
  // THIS WILL OVERRIDE THE NUMBER AUTOMATICALLY CALCULATED BY THE SIMULATION
  // Override the simulation definition by specifying how many records are required from ElasticSearch for the test
  // Default = -1 (no override)
  val RECORDS_REQUIRED_OVERRIDE = -1 //Default = -1

  // If OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE is set to true, the simulation will use case IDs from the resources/caseIds.csv
  // file, instead of fetching cases from ElasticSearch. This can be useful when running locally to save tunnelling to ES
  // If set to true, the file must contain a header line named caseId, followed by a list of CCD case IDs
  var OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE = false

}
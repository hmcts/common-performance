package elasticSearchFeeder

object TestElasticSearchFeederConfig {
  def set(overrides: ElasticSearchFeederConfigDefaultValues): Unit = {
    ElasticSearchFeederConfig.set(overrides)
  }

  def overrideCsvFile(enabled: Boolean): Unit = {
    set(ElasticSearchFeederConfigDefaultValues(OVERRIDE_ELASTICSEARCH_WITH_CSV_FILE = enabled))
  }

  def overrideRecordsRequired(count: Int): Unit = {
    set(ElasticSearchFeederConfigDefaultValues(RECORDS_REQUIRED_OVERRIDE = count))
  }
}

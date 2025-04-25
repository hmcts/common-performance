package elasticSearchFeeder

object CalculateRecordsRequired {

  def calculate(targetIterationsPerHour: Double, rampUpDurationMins: Int, testDurationMins: Int, rampDownDurationMins: Int) = {

    val recordsRequiredCalculated: Int =
      math.round(targetIterationsPerHour / 60 * rampUpDurationMins / 2).toInt +
        math.round(targetIterationsPerHour / 60 * testDurationMins).toInt +
        math.round(targetIterationsPerHour / 60 * rampDownDurationMins / 2).toInt

    val recordsRequiredOverride = ElasticSearchFeederConfig.config.RECORDS_REQUIRED_OVERRIDE

    val elasticSearchRecordsToRequest = if (recordsRequiredOverride == -1) {
      recordsRequiredCalculated // return the calculated value
    } else {
      println("INFO: Automatic calculation of data required has been manually overridden in the config - requesting "
        + recordsRequiredOverride + " records from ElasticSearch")
      if (recordsRequiredOverride < recordsRequiredCalculated) {
        println("WARNING: Override value (" + recordsRequiredOverride +
          ") is lower than iteration calculation (" + recordsRequiredCalculated + "). You may not have sufficient data for your simulation")
      }
      recordsRequiredOverride // return the override value
    }

  }
}

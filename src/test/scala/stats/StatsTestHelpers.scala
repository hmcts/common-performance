package stats

import java.io.File

trait StatsTestHelpers {
  def loadDummyStatsFile(fileName: String): File = {
    new File(getClass.getClassLoader.getResource(fileName).toURI)
  }

  val outputBaseDir = "build/reports/gatling/tests"
}

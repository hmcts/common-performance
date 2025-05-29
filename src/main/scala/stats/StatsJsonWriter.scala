package stats

import io.gatling.commons.stats.Status
import io.gatling.core.stats.StatsEngine
import io.gatling.core.scenario.Simulation
import io.gatling.core.session.GroupBlock

import java.io.{BufferedWriter, File, FileWriter}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._

/**
 * Automatically collect request stats and write stats.json at the end of the run.
 * Just add the following code to the Gatling simulation:
 *
 * before {
 *   stats.StatsJsonWriter.enableJsonStats()
 * }
 *
 */
trait StatsJsonWriter extends Simulation {

  private val results = new ConcurrentLinkedQueue[String]()
  private var writer: BufferedWriter = _

  before {
    val resultsFolder = System.getProperty("gatling.resultsFolder")
    val outputFile = new File(resultsFolder, "stats.json")
    writer = new BufferedWriter(new FileWriter(outputFile))
    writer.write("{\n  \"requests\": [\n")
  }

  after {
    val jsonLines = results.asScala.mkString(",\n")
    writer.write(jsonLines)
    writer.write("\n  ]\n}")
    writer.close()
  }

  def logRequest(
                  name: String,
                  group: List[String],
                  status: Status,
                  startTimestamp: Long,
                  endTimestamp: Long
                ): Unit = {
    val json =
      s"""    {
         |      "name": "${name.replace("\"", "\\\"")}",
         |      "group": "${group.mkString(" / ").replace("\"", "\\\"")}",
         |      "status": "$status",
         |      "startTimestamp": $startTimestamp,
         |      "endTimestamp": $endTimestamp,
         |      "responseTime": ${endTimestamp - startTimestamp}
         |    }""".stripMargin
    results.add(json)
  }

  def withStatsJson(engine: StatsEngine): StatsEngine = new StatsEngine {

    override def logResponse(
                              scenario: String,
                              groups: List[String],
                              requestName: String,
                              startTimestamp: Long,
                              endTimestamp: Long,
                              status: Status,
                              responseCode: Option[String],
                              message: Option[String]
                            ): Unit = {
      logRequest(requestName, groups, status, startTimestamp, endTimestamp)
      engine.logResponse(scenario, groups, requestName, startTimestamp, endTimestamp, status, responseCode, message)
    }

    override def logUserStart(scenario: String): Unit =
      engine.logUserStart(scenario)

    override def logUserEnd(scenario: String): Unit =
      engine.logUserEnd(scenario)

    override def logGroupEnd(scenario: String, group: GroupBlock, timestamp: Long): Unit =
      engine.logGroupEnd(scenario, group, timestamp)
  }

}

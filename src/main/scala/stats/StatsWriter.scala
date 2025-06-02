package stats

import java.io.{File, PrintWriter}

case class MetricValues(total: Double, ok: Double, ko: Double)
case class GroupValues(count: Int, percentage: Double)

object StatsWriter {

  def createFakeSimulation(name: String, numberOfRequests: MetricValues, minResponseTime: MetricValues,
                           maxResponseTime: MetricValues, meanResponseTime: MetricValues,
                           standardDeviation: MetricValues, percentiles1: MetricValues,
                           percentiles2: MetricValues, percentiles3: MetricValues,
                           percentiles4: MetricValues, group1: GroupValues,
                           group2: GroupValues, group3: GroupValues, group4: GroupValues,
                           meanNumberOfRequestsPerSecond: MetricValues, outputDirPath: String): Unit = {

    val dir = new File(outputDirPath)
    dir.mkdirs()

    val outputFile = new File(dir, "global_stats.json")
    val writer = new PrintWriter(outputFile)

    val json =
      s"""{
         |  "name": "All Requests",
         |  "numberOfRequests": { "total": ${numberOfRequests.total}, "ok": ${numberOfRequests.ok}, "ko": ${numberOfRequests.ko} },
         |  "minResponseTime": { "total": ${minResponseTime.total}, "ok": ${minResponseTime.ok}, "ko": ${minResponseTime.ko} },
         |  "maxResponseTime": { "total": ${maxResponseTime.total}, "ok": ${maxResponseTime.ok}, "ko": ${maxResponseTime.ko} },
         |  "meanResponseTime": { "total": ${meanResponseTime.total}, "ok": ${meanResponseTime.ok}, "ko": ${meanResponseTime.ko} },
         |  "standardDeviation": { "total": ${standardDeviation.total}, "ok": ${standardDeviation.ok}, "ko": ${standardDeviation.ko} },
         |  "percentiles1": { "total": ${percentiles1.total}, "ok": ${percentiles1.ok}, "ko": ${percentiles1.ko} },
         |  "percentiles2": { "total": ${percentiles2.total}, "ok": ${percentiles2.ok}, "ko": ${percentiles2.ko} },
         |  "percentiles3": { "total": ${percentiles3.total}, "ok": ${percentiles3.ok}, "ko": ${percentiles3.ko} },
         |  "percentiles4": { "total": ${percentiles4.total}, "ok": ${percentiles4.ok}, "ko": ${percentiles4.ko} },
         |  "group1": { "name": "t < 800 ms", "htmlName": "t < 800 ms", "count": ${group1.count}, "percentage": ${group1.percentage} },
         |  "group2": { "name": "800 ms <= t < 1200 ms", "htmlName": "t >= 800 ms <br> t < 1200 ms", "count": ${group2.count}, "percentage": ${group2.percentage} },
         |  "group3": { "name": "t >= 1200 ms", "htmlName": "t >= 1200 ms", "count": ${group3.count}, "percentage": ${group3.percentage} },
         |  "group4": { "name": "failed", "htmlName": "failed", "count": ${group4.count}, "percentage": ${group4.percentage} },
         |  "meanNumberOfRequestsPerSecond": { "total": ${meanNumberOfRequestsPerSecond.total}, "ok": ${meanNumberOfRequestsPerSecond.ok}, "ko": ${meanNumberOfRequestsPerSecond.ko} }
         |}""".stripMargin

    writer.write(json)
    writer.close()

    println(s"[StatsGenerator] ✅ Created global_stats.json for [$name] in $outputDirPath")
  }
}

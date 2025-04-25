package elasticSearchFeeder

import java.util.concurrent.ConcurrentLinkedQueue
import scala.util.Random

// Unified feeder for QUEUE, SHUFFLE, CIRCULAR, and RANDOM strategies
class CaseIdFeeder(caseIds: List[String], feederType: FeederType) {

  val feeder: Iterator[Map[String, String]] = feederType match {

    case FeederType.QUEUE | FeederType.SHUFFLE | FeederType.CIRCULAR =>
      val queue = new ConcurrentLinkedQueue[Map[String, String]]()

      val caseIdList = if (feederType == FeederType.SHUFFLE) Random.shuffle(caseIds) else caseIds
      caseIdList.foreach { caseId =>
        queue.offer(Map("caseId" -> caseId))
        println(s"CASE ID (${feederType}): $caseId") //DEBUG LOG: Outputs the list of cases in the feeder
      }

      Iterator.continually {
        val nextCaseId = Option(queue.poll())
        feederType match {
          case FeederType.CIRCULAR =>
            nextCaseId match {
              case Some(caseId) =>
                queue.offer(caseId)
                caseId
              case None =>
                println("ERROR: Circular feeder ran unexpectedly dry.")
                throw new RuntimeException("Feeder is unexpectedly empty in CIRCULAR mode.")
            }

          case _ =>
            nextCaseId match {
              case Some(caseId) => caseId
              case None =>
                println(s"ERROR: Feeder exhausted for type $feederType. Aborting user.")
                throw new RuntimeException(s"Feeder exhausted for $feederType")
            }
        }
      }

    case FeederType.RANDOM =>
      val caseIdList = caseIds.map(caseId => Map("caseId" -> caseId))
      caseIdList.foreach(caseId => println(s"CASE ID (RANDOM): ${caseId("caseId")}"))

      Iterator.continually {
        if (caseIdList.isEmpty) {
          println("ERROR: Random feeder is empty. Aborting user.")
          throw new RuntimeException("Feeder is empty in RANDOM mode.")
        } else {
          caseIdList(Random.nextInt(caseIdList.size))
        }
      }

    case _ =>
      println(s"ERROR: Invalid feederType '$feederType'. Valid types: QUEUE, CIRCULAR, SHUFFLE, RANDOM.")
      throw new IllegalArgumentException(s"Invalid feederType: $feederType")
  }
}

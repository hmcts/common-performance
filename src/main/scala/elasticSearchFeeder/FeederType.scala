package elasticSearchFeeder

sealed trait FeederType
object FeederType {
    case object QUEUE extends FeederType
    case object CIRCULAR extends FeederType
    case object RANDOM extends FeederType
    case object SHUFFLE extends FeederType

    val values: List[FeederType] = List(QUEUE, CIRCULAR, RANDOM, SHUFFLE)
}
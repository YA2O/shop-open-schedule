package bzh.ya2o.openinghours.model

import cats.data.Validated

sealed abstract class EventType

object EventType {
  val Values: List[EventType] = List(Open, Close)

  final case object Open extends EventType
  final case object Close extends EventType

  def apply(name: String): Validated[String, EventType] =
    Validated.fromOption(
      byName.get(name.toLowerCase),
      s"invalid EventType name: [$name]; " +
        s"expected name for EventType to be one of ${Values.map(v => s"[${v.toString}]").mkString(", ")}"
    )

  private val byName: Map[String, EventType] = Values.map { eventType =>
    eventType.toString.toLowerCase -> eventType
  }.toMap
}

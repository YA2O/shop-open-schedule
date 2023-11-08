package bzh.ya2o.openinghours.model

import java.time.DayOfWeek
import java.time.LocalTime

final case class Event(
  day: DayOfWeek,
  time: LocalTime,
  eventType: EventType
)

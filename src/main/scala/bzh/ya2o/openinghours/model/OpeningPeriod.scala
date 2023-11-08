package bzh.ya2o.openinghours.model

import cats.data.Validated
import cats.implicits._

final case class OpeningPeriod private[OpeningPeriod] (opening: TimeOfWeek, closing: TimeOfWeek)

object OpeningPeriod {
  def apply(opening: TimeOfWeek, closing: TimeOfWeek): Validated[String, OpeningPeriod] = {
    validate(opening, closing).map((new OpeningPeriod(_, _)).tupled)
  }

  private def validate(
    opening: TimeOfWeek,
    closing: TimeOfWeek
  ): Validated[String, (TimeOfWeek, TimeOfWeek)] = {
    if (opening.day == closing.day) {
      if (opening.time.isBefore(closing.time)) {
        (opening, closing).valid
      } else
        s"opening must be before closing: [$opening] - [$closing]".invalid
    } else if (opening.day.plus(1) == closing.day) {
      if (opening.time.isAfter(closing.time)) {
        (opening, closing).valid
      } else {
        s"opening must be less than 24 hours: [$opening] - [$closing]".invalid
      }
    } else {
      s"opening and closing must have same day, or consecutive days: [$opening] - [$closing]".invalid
    }
  }
}

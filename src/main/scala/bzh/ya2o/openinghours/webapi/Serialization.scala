package bzh.ya2o.openinghours.webapi

import bzh.ya2o.openinghours.model.OpeningPeriod
import io.circe.Encoder

import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import scala.collection.immutable.SortedMap

object Serialization {

  final case class Output(openings: List[String])
  object Output {
    implicit val encoder: Encoder[Output] = Encoder.forProduct1("opening-hours")(_.openings)
  }

  final case class ErrorMessageOutput private[ErrorMessageOutput] (msg: String, cause: String)

  object ErrorMessageOutput {
    def apply(err: Throwable): ErrorMessageOutput = {
      ErrorMessageOutput(
        Option(err.getMessage).getOrElse(err.toString),
        Option(err.getCause)
          .flatMap { c =>
            Option(c.getMessage)
          }
          .getOrElse("")
      )
    }

    implicit val encoder: Encoder[ErrorMessageOutput] =
      Encoder.forProduct2("message", "cause") { err =>
        (err.msg, err.cause)
      }
  }

  def toReadableOpeningHours(openingPeriodsByDay: SortedMap[DayOfWeek, List[OpeningPeriod]]): Output =
    Output({
      openingPeriodsByDay.map { case (day, openingPeriods) =>
        toReadableDayOpeningPeriods(day, openingPeriods)
      }
    }.toList)

  private def toReadableDayOpeningPeriods(day: DayOfWeek, openings: List[OpeningPeriod]): String = {
    s"${day.name.toLowerCase.capitalize}: ${if (openings.isEmpty) "Closed"
    else { openings.map(toReadableOpeningPeriod).mkString(", ") }}"
  }

  private def toReadableOpeningPeriod(openingPeriod: OpeningPeriod): String = {
    s"${toReadableTime(openingPeriod.opening.time)} - ${toReadableTime(openingPeriod.closing.time)}"
  }

  private def toReadableTime(time: LocalTime): String = {
    val formatterPlainHours = DateTimeFormatter.ofPattern("h a")
    val formatterWithMinutes = DateTimeFormatter.ofPattern("h:mm a")

    if (time.getMinute == 0) {
      time.format(formatterPlainHours).toUpperCase
    } else {
      time.format(formatterWithMinutes).toUpperCase
    }
  }

}
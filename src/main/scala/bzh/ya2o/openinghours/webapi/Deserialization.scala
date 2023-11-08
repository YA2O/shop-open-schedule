package bzh.ya2o.openinghours.webapi

import bzh.ya2o.openinghours.model.EventType
import cats.implicits.toBifunctorOps
import io.circe.generic.semiauto.deriveDecoder
import io.circe.Decoder
import io.circe.KeyDecoder

import java.time.DayOfWeek
import java.time.LocalTime
import scala.util.Try

object Deserialization {

  type Input = Map[DayOfWeek, List[EventAtTime]]

  final case class EventAtTime(`type`: EventType, value: LocalTime) {
    def time: LocalTime = value
  }

  implicit val keyDec: KeyDecoder[DayOfWeek] = (name: String) =>
    Try(DayOfWeek.valueOf(name.toUpperCase)).toOption
  implicit val localTimeDec: Decoder[LocalTime] = Decoder[Int].emap { i =>
    if (i >= 0 && i <= 86399) {
      Try(LocalTime.ofSecondOfDay(i.toLong)).toEither.leftMap { err =>
        s"error converting [$i] to a local time: [$err]"
      }
    } else {
      Left(s"invalid time: [$i]")
    }
  }
  implicit val eventTypeDec: Decoder[EventType] = Decoder[String].emap(EventType(_).toEither)
  implicit val eventDec: Decoder[EventAtTime] = deriveDecoder[EventAtTime]
}

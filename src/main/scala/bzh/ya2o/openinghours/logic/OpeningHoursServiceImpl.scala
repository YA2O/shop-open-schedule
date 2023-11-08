package bzh.ya2o.openinghours.logic

import bzh.ya2o.openinghours.application.error.AppError.BadRequestError
import bzh.ya2o.openinghours.application.logging.FunctionalLogger
import bzh.ya2o.openinghours.model.Event
import bzh.ya2o.openinghours.model.EventList
import bzh.ya2o.openinghours.model.OpeningPeriod
import bzh.ya2o.openinghours.model.TimeOfWeek
import bzh.ya2o.openinghours.webapi.Deserialization
import bzh.ya2o.openinghours.webapi.Deserialization.Input
import cats.effect.IO
import cats.implicits._

import java.time.DayOfWeek
import scala.collection.immutable.SortedMap
import scala.util.Try

final class OpeningHoursServiceImpl(logger: FunctionalLogger) extends OpeningHoursService {

  override def makeOpeningPeriodsByDay(
    input: Input
  ): IO[SortedMap[DayOfWeek, List[OpeningPeriod]]] = {
    for {
      eventList <- IO.fromTry(makeEventList(input))
      _ <- logger.debug(s"event list: [$eventList]")
      openingPeriods <- IO.fromTry(makeOpeningPeriods(eventList))
      _ <- logger.debug(s"opening periods: [$openingPeriods]")
    } yield {
      makeOpeningPeriodsByDay(openingPeriods)
    }
  }

  private[this] def makeEventList(input: Input): Try[EventList] = {
    val events: List[Event] = input.flatMap {
      case (day: DayOfWeek, evnts: List[Deserialization.EventAtTime]) =>
        evnts.map { evt: Deserialization.EventAtTime =>
          Event(day, evt.time, evt.`type`)
        }
    }.toList
    EventList(events).toEither.leftMap(BadRequestError).toTry
  }

  private[this] def makeOpeningPeriods(eventList: EventList): Try[List[OpeningPeriod]] = {
    eventList.events
      .grouped(2)
      .collect { case List(evnt1, evnt2) =>
        makeOpeningPeriod(evnt1, evnt2)
      }
      .toList
      .sequence
  }

  private[this] def makeOpeningPeriod(event1: Event, event2: Event): Try[OpeningPeriod] = {
    for {
      openingPeriod <- OpeningPeriod(
        TimeOfWeek(event1.day, event1.time),
        TimeOfWeek(event2.day, event2.time)
      ).toEither.leftMap(BadRequestError).toTry
    } yield {
      openingPeriod
    }
  }

  private[this] def makeOpeningPeriodsByDay(
    openingPeriods: List[OpeningPeriod]
  ): SortedMap[DayOfWeek, List[OpeningPeriod]] = {
    DayOfWeek
      .values()
      .map { day =>
        day -> openingPeriods.filter(_.opening.day == day)
      }
      .toMap
      .to(SortedMap)
  }

}
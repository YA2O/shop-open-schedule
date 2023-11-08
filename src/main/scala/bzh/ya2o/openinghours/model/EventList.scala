package bzh.ya2o.openinghours.model

import cats.data.Validated
import cats.implicits._

import scala.annotation.tailrec

final case class EventList private[EventList] (events: List[Event])

object EventList {
  def apply(events: List[Event]): Validated[String, EventList] = {
    validate(events).map(new EventList(_))
  }

  private def validate(events: List[Event]): Validated[String, List[Event]] = {
    @tailrec
    def checkPairability(events: List[Event]): Boolean = {
      events match {
        case Event(_, _, EventType.Open) :: Event(_, _, EventType.Close) :: tail => checkPairability(tail)
        case Nil => true
        case _ => false
      }
    }

    val sorted = events.sortBy { event =>
      (event.day, event.time)
    }
    val reordered = sorted match {
      case head :: tail if head.eventType == EventType.Close => tail :+ head
      case _ => sorted
    }
    if (checkPairability(reordered)) reordered.valid
    else "can't pair events; overlapping opening periods, missing event, wrong event order?".invalid
  }
}

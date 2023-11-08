package bzh.ya2o.openinghours.logic

import bzh.ya2o.openinghours.application.logging.Logger4j
import bzh.ya2o.openinghours.model.OpeningPeriod
import bzh.ya2o.openinghours.webapi.Deserialization.Input
import cats.effect.IO
import org.log4s.getLogger

import java.time.DayOfWeek
import scala.collection.immutable.SortedMap

trait OpeningHoursService {
  def makeOpeningPeriodsByDay(input: Input): IO[SortedMap[DayOfWeek, List[OpeningPeriod]]]

}

object OpeningHoursService {
  def apply(): IO[OpeningHoursService] = IO {
    val logger = new Logger4j(getLogger)
    new OpeningHoursServiceImpl(logger)
  }
}

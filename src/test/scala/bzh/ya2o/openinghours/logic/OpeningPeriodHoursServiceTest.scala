package bzh.ya2o.openinghours.logic

import bzh.ya2o.openinghours.application.error.AppError.BadRequestError
import bzh.ya2o.openinghours.webapi.Deserialization
import bzh.ya2o.openinghours.webapi.Deserialization.EventAtTime
import bzh.ya2o.openinghours.logic.OpeningPeriodHoursServiceTest.TestFixture
import bzh.ya2o.openinghours.model.EventType
import bzh.ya2o.openinghours.model.OpeningPeriod
import bzh.ya2o.openinghours.model.TimeOfWeek
import cats.effect.unsafe.IORuntime
import cats.effect.IO
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Inside

import java.time.DayOfWeek
import java.time.DayOfWeek._
import java.time.LocalTime
import scala.collection.immutable.SortedMap

class OpeningPeriodHoursServiceTest extends AnyFreeSpec with Matchers with Inside {

  "OpeningHoursService" - {
    "should convert an input" - {
      "as in the specification" in new TestFixture {
        // Arrange
        val input: Map[DayOfWeek, List[Deserialization.EventAtTime]] =
          Map(
            TUESDAY -> List(
              EventAtTime(EventType.Open, LocalTime.parse("10:00")),
              EventAtTime(EventType.Close, LocalTime.parse("18:00"))
            ),
            THURSDAY -> List(
              EventAtTime(EventType.Open, LocalTime.parse("10:30")),
              EventAtTime(EventType.Close, LocalTime.parse("18:00"))
            ),
            FRIDAY -> List(
              EventAtTime(EventType.Open, LocalTime.parse("10:00"))
            ),
            SATURDAY -> List(
              EventAtTime(EventType.Close, LocalTime.parse("01:00")),
              EventAtTime(EventType.Open, LocalTime.parse("10:00"))
            ),
            SUNDAY -> List(
              EventAtTime(EventType.Close, LocalTime.parse("01:00")),
              EventAtTime(EventType.Open, LocalTime.parse("12:00")),
              EventAtTime(EventType.Close, LocalTime.parse("21:00"))
            )
          )
        val expected: SortedMap[DayOfWeek, List[OpeningPeriod]] = SortedMap(
          MONDAY -> Nil,
          TUESDAY -> List(
            OpeningPeriod(
              TimeOfWeek(TUESDAY, LocalTime.parse("10:00")),
              TimeOfWeek(TUESDAY, LocalTime.parse("18:00"))
            ).toOption.get
          ),
          WEDNESDAY -> Nil,
          THURSDAY -> List(
            OpeningPeriod(
              TimeOfWeek(THURSDAY, LocalTime.parse("10:30")),
              TimeOfWeek(THURSDAY, LocalTime.parse("18:00"))
            ).toOption.get
          ),
          FRIDAY -> List(
            OpeningPeriod(
              TimeOfWeek(FRIDAY, LocalTime.parse("10:00")),
              TimeOfWeek(SATURDAY, LocalTime.parse("01:00"))
            ).toOption.get
          ),
          SATURDAY -> List(
            OpeningPeriod(
              TimeOfWeek(SATURDAY, LocalTime.parse("10:00")),
              TimeOfWeek(SUNDAY, LocalTime.parse("01:00"))
            ).toOption.get
          ),
          SUNDAY -> List(
            OpeningPeriod(
              TimeOfWeek(SUNDAY, LocalTime.parse("12:00")),
              TimeOfWeek(SUNDAY, LocalTime.parse("21:00"))
            ).toOption.get
          )
        )

        // Act
        val result: IO[SortedMap[DayOfWeek, List[OpeningPeriod]]] = service.makeOpeningPeriodsByDay(input)

        // Assert
        result
          .map { output: SortedMap[DayOfWeek, List[OpeningPeriod]] =>
            output shouldBe expected
          }
          .unsafeRunSync()
      }

      "with a 1st event being a closing" in new TestFixture {
        // Arrange
        val input: Map[DayOfWeek, List[Deserialization.EventAtTime]] =
          Map(
            MONDAY -> List(
              EventAtTime(EventType.Close, LocalTime.parse("01:00")),
              EventAtTime(EventType.Open, LocalTime.parse("10:00")),
              EventAtTime(EventType.Close, LocalTime.parse("13:00"))
            ),
            SUNDAY -> List(
              EventAtTime(EventType.Open, LocalTime.parse("22:00"))
            )
          )
        val expected: SortedMap[DayOfWeek, List[OpeningPeriod]] = SortedMap(
          MONDAY -> List(
            OpeningPeriod(
              TimeOfWeek(MONDAY, LocalTime.parse("10:00")),
              TimeOfWeek(MONDAY, LocalTime.parse("13:00"))
            ).toOption.get
          ),
          TUESDAY -> Nil,
          WEDNESDAY -> Nil,
          THURSDAY -> Nil,
          FRIDAY -> Nil,
          SATURDAY -> Nil,
          SUNDAY -> List(
            OpeningPeriod(
              TimeOfWeek(SUNDAY, LocalTime.parse("22:00")),
              TimeOfWeek(MONDAY, LocalTime.parse("01:00"))
            ).toOption.get
          )
        )

        // Act
        val result: IO[SortedMap[DayOfWeek, List[OpeningPeriod]]] = service.makeOpeningPeriodsByDay(input)

        // Assert
        result
          .map { output: SortedMap[DayOfWeek, List[OpeningPeriod]] =>
            output shouldBe expected
          }
          .unsafeRunSync()
      }
    }

    "should fail to convert an input" - {
      "with consecutive openings" in new TestFixture {
        // Arrange
        val input: Map[DayOfWeek, List[EventAtTime]] =
          Map(
            MONDAY -> List(
              EventAtTime(EventType.Open, LocalTime.parse("10:00")),
              EventAtTime(EventType.Open, LocalTime.parse("11:00"))
            )
          )
        // Act
        val result: IO[SortedMap[DayOfWeek, List[OpeningPeriod]]] = service.makeOpeningPeriodsByDay(input)

        // Assert
        result.attempt
          .map(inside(_) { case Left(error) =>
            error shouldBe a[BadRequestError]
          })
          .unsafeRunSync()
      }

      "with consecutive closings" in new TestFixture {
        // Arrange
        val input: Map[DayOfWeek, List[EventAtTime]] =
          Map(
            MONDAY -> List(
              EventAtTime(EventType.Close, LocalTime.parse("10:00")),
              EventAtTime(EventType.Close, LocalTime.parse("11:00"))
            )
          )
        // Act
        val result: IO[SortedMap[DayOfWeek, List[OpeningPeriod]]] = service.makeOpeningPeriodsByDay(input)

        // Assert
        result.attempt
          .map(inside(_) { case Left(error) =>
            error shouldBe a[BadRequestError]
          })
          .unsafeRunSync()
      }

      "with 24 hours or more between opening and closing" in new TestFixture {
        // Arrange
        val input: Map[DayOfWeek, List[EventAtTime]] =
          Map(
            MONDAY -> List(
              EventAtTime(EventType.Open, LocalTime.parse("10:00"))
            ),
            TUESDAY -> List(
              EventAtTime(EventType.Close, LocalTime.parse("10:00"))
            )
          )
        // Act
        val result: IO[SortedMap[DayOfWeek, List[OpeningPeriod]]] = service.makeOpeningPeriodsByDay(input)

        // Assert
        result.attempt
          .map(inside(_) { case Left(error) =>
            error shouldBe a[BadRequestError]
          })
          .unsafeRunSync()
      }

      "with overlapping opening periods" in new TestFixture {
        // Arrange
        val input: Map[DayOfWeek, List[EventAtTime]] =
          Map(
            MONDAY -> List(
              EventAtTime(EventType.Open, LocalTime.parse("10:00")),
              EventAtTime(EventType.Close, LocalTime.parse("12:00")),
              EventAtTime(EventType.Open, LocalTime.parse("11:00")),
              EventAtTime(EventType.Close, LocalTime.parse("11:30"))
            )
          )
        // Act
        val result: IO[SortedMap[DayOfWeek, List[OpeningPeriod]]] = service.makeOpeningPeriodsByDay(input)

        // Assert
        result.attempt
          .map(inside(_) { case Left(error) =>
            error shouldBe a[BadRequestError]
          })
          .unsafeRunSync()
      }
    }
  }
}

private object OpeningPeriodHoursServiceTest {
  trait TestFixture {
    implicit val runtime: IORuntime = IORuntime.global

    final lazy val service: OpeningHoursService = OpeningHoursService().unsafeRunSync()
  }
}

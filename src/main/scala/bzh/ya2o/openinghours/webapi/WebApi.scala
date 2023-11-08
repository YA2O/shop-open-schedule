package bzh.ya2o.openinghours.webapi
import bzh.ya2o.openinghours.application.error.AppError
import bzh.ya2o.openinghours.application.error.AppError._
import bzh.ya2o.openinghours.application.logging.FunctionalLogger
import bzh.ya2o.openinghours.application.logging.Logger4j
import bzh.ya2o.openinghours.webapi.Deserialization._
import bzh.ya2o.openinghours.webapi.Serialization.ErrorMessageOutput
import bzh.ya2o.openinghours.logic.OpeningHoursService
import bzh.ya2o.openinghours.webapi.Serialization.Output
import cats.effect.IO
import org.http4s.dsl.Http4sDsl
import org.http4s.EntityDecoder
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder
import org.http4s.EntityEncoder
import org.http4s.Request
import org.http4s.circe
import org.log4s.getLogger

final class WebApi(
  service: OpeningHoursService,
  logger: FunctionalLogger
) {
  private[this] val dsl = new Http4sDsl[IO] {}
  import dsl._

  def routes(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] { case req @ POST -> Root / "opening-hours" / "v1" =>
      withErrorHandling(generateHumanReadableOutput(req))
    }
  }

  private[this] def generateHumanReadableOutput(request: Request[IO]): IO[Response[IO]] = {

    implicit val inputDec: EntityDecoder[IO, Input] = circe.accumulatingJsonOf[IO, Input]
    implicit val outputEncoder: EntityEncoder[IO, Output] = CirceEntityEncoder.circeEntityEncoder

    for {
      input <- request.as[Input]
      _ <- logger.info(s"Received input: [$input]")
      openingPeriodsByDay <- service
        .makeOpeningPeriodsByDay(
          input
        )
      resp <- Ok(Serialization.toReadableOpeningHours(openingPeriodsByDay))
    } yield resp
  }

  private[this] def withErrorHandling(response: IO[Response[IO]]): IO[Response[IO]] = {
    implicit val entityEncoder: EntityEncoder[IO, ErrorMessageOutput] =
      CirceEntityEncoder.circeEntityEncoder

    response.handleErrorWith {
      case err: org.http4s.InvalidMessageBodyFailure =>
        BadRequest(ErrorMessageOutput(err))
      case err: org.http4s.MalformedMessageBodyFailure =>
        BadRequest(ErrorMessageOutput(err))
      case err: AppError =>
        err match {
          case _: BadRequestError => BadRequest(ErrorMessageOutput(err))
        }
      case throwable: Throwable =>
        logger.error(throwable) >>
          InternalServerError(ErrorMessageOutput(s"Unhandled error!", throwable.toString))
    }
  }

}

object WebApi {
  def apply(service: OpeningHoursService): IO[WebApi] = IO {
    val logger = new Logger4j(getLogger)
    new WebApi(service, logger)
  }
}

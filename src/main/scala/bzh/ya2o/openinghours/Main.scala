package bzh.ya2o.openinghours

import bzh.ya2o.openinghours.application.config.Config
import bzh.ya2o.openinghours.application.logging.FunctionalLogger
import bzh.ya2o.openinghours.application.logging.Logger4j
import bzh.ya2o.openinghours.webapi.WebApi
import bzh.ya2o.openinghours.logic.OpeningHoursService
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import com.comcast.ip4s._
import org.http4s.server.Server
import org.http4s.server.middleware.Logger
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.log4s.getLogger

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    for {
      logger <- IO(new Logger4j(getLogger))
      exitCode <- createServer(logger).useForever
        .onError { throwable =>
          logger.error(throwable)
        }
        .as(ExitCode.Success)
    } yield {
      exitCode
    }
  }

  private def createServer(logger: FunctionalLogger): Resource[IO, Server] = {
    for {
      config <- Resource.eval(Config.config.load)
      _ <- Resource.eval(logger.info(s"Config: [$config]"))
      service <- Resource.eval(OpeningHoursService())
      webApi <- Resource.eval(WebApi(service))
      httpApp: HttpApp[IO] = Logger.httpApp(
        logHeaders = true,
        logBody = true
      )(
        webApi.routes().orNotFound
      )
      port <- Resource.eval(
        IO(
          Port
            .fromInt(config.server.port.value)
            .getOrElse(throw new IllegalArgumentException(s"Invalid port: [${config.server.port}]."))
        )
      )
      server <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString("0.0.0.0").get)
        .withPort(port)
        .withHttpApp(httpApp)
        .build
    } yield server
  }

}
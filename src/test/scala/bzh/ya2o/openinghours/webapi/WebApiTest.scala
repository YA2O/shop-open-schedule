package bzh.ya2o.openinghours.webapi
import bzh.ya2o.openinghours.logic.OpeningHoursService
import bzh.ya2o.openinghours.webapi.WebApiTest.TestFixture
import cats.effect.unsafe.IORuntime
import cats.effect.IO
import io.circe.literal._
import io.circe.Json
import org.http4s.Response
import org.http4s.Status
import org.http4s.implicits._
import org.http4s.EntityDecoder
import org.http4s.Method
import org.http4s.Request
import org.http4s.circe
import org.scalatest.freespec.AnyFreeSpec
import io.circe.parser.decode
import scala.io.Source

class WebApiTest extends AnyFreeSpec {

  "WebApi should" - {
    "return the correct result for the 1st example (from specification)" in new TestFixture {
      // Arrange
      val input: String = Source.fromResource("input1.json").mkString
      val expected: Json = decode[Json](Source.fromResource("output1.json").mkString).toOption.get

      // Act
      val resp: Response[IO] = applyRequest(webApi)(input)

      // Assert
      assert(
        resp.status == Status.Ok &&
          resp.as[Json].unsafeRunSync() == expected
      )
    }
    "return the correct result for the 2nd example (with arbitrary casing, and arbitrary ordering)" in
      new TestFixture {
        // Arrange
        val input: String = Source.fromResource("input2.json").mkString
        val expected: Json = decode[Json](Source.fromResource("output2.json").mkString).toOption.get

        // Act
        val resp: Response[IO] = applyRequest(webApi)(input)

        // Assert
        assert(
          resp.status == Status.Ok &&
            resp.as[Json].unsafeRunSync() == expected
        )
      }

    "respond with 400 if the input is not JSON" in new TestFixture {
      // Arrange
      val input: String = "Not json"

      // Act
      val resp: Response[IO] = applyRequest(webApi)(input)

      // Assert
      assert(resp.status == Status.BadRequest)
    }

    "respond with 400 if the input is JSON but invalid" in new TestFixture {
      // Arrange
      val input = json"""{"hello":"world"}"""

      // Act
      val resp: Response[IO] = applyRequest(webApi)(input.toString)

      // Assert
      assert(resp.status == Status.BadRequest)
    }
  }

  private def applyRequest(webApi: WebApi)(input: String)(implicit runtime: IORuntime): Response[IO] = {
    webApi
      .routes()
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"/opening-hours/v1").withEntity(input)
      )
      .unsafeRunSync()
  }
}

private object WebApiTest {
  trait TestFixture {
    implicit val runtime: IORuntime = IORuntime.global

    //implicit val decoder: Decoder[Output] = deriveDecoder[Output]
    implicit val entityDecoder: EntityDecoder[IO, Json] = circe.accumulatingJsonOf[IO, Json]

    final lazy val webApi: WebApi = (
      for {
        service <- OpeningHoursService()
        api <- WebApi(service)
      } yield api
    ).unsafeRunSync()
  }
}

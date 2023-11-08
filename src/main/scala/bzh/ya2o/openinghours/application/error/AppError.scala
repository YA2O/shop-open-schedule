package bzh.ya2o.openinghours.application.error

import scala.util.control.NoStackTrace

sealed abstract class AppError(val message: String) extends Throwable(message) with NoStackTrace

object AppError {
  final case class BadRequestError(override val message: String) extends AppError(message)
}

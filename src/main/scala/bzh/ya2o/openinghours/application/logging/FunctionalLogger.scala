package bzh.ya2o.openinghours.application.logging

import cats.effect.IO

trait FunctionalLogger {
  def error(throwable: Throwable): IO[Unit]
  def warn(msg: String): IO[Unit]
  def info(msg: String): IO[Unit]
  def debug(msg: String): IO[Unit]
}

class Logger4j(logger: org.log4s.Logger) extends FunctionalLogger {
  override def error(throwable: Throwable): IO[Unit] = IO(logger.error(throwable)(throwable.getMessage))
  override def warn(msg: String): IO[Unit] = IO(logger.warn(msg))
  override def info(msg: String): IO[Unit] = IO(logger.info(msg))
  override def debug(msg: String): IO[Unit] = IO(logger.debug(msg))
}

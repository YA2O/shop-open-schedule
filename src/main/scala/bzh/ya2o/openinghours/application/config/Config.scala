package bzh.ya2o.openinghours.application.config

import cats.effect.IO
import ciris.ConfigValue

final case class Config(
  server: ServerConfig
)

object Config {
  def config: ConfigValue[IO, Config] = {
    ServerConfig.config.map(Config.apply)
  }
}

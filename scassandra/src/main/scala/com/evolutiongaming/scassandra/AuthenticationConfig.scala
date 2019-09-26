package com.evolutiongaming.scassandra

import com.typesafe.config.Config
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/auth/]]
  */
final case class AuthenticationConfig(username: String, password: String)

object AuthenticationConfig {

  implicit val configReaderAuthenticationConfig: ConfigReader[AuthenticationConfig] = deriveReader

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): AuthenticationConfig = {
    ConfigSource.fromConfig(config).loadOrThrow[AuthenticationConfig]
  }
}

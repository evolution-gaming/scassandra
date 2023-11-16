package com.evolutiongaming.scassandra

import com.typesafe.config.Config
import pureconfig.ConfigSource

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/auth/]]
  */
final case class AuthenticationConfig(username: String, password: Masked[String])

object AuthenticationConfig extends AuthenticationConfigImplicits {

  def apply(username: String, password: String): AuthenticationConfig = {
    AuthenticationConfig(username, Masked(password))
  }

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): AuthenticationConfig = {
    ConfigSource.fromConfig(config).loadOrThrow[AuthenticationConfig]
  }
}

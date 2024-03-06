package com.evolutiongaming.scassandra

import pureconfig.ConfigReader

trait AuthenticationConfigImplicits {
  implicit val configReaderAuthenticationConfig: ConfigReader[AuthenticationConfig] = 
    ConfigReader.forProduct2[AuthenticationConfig, String, Masked[String]]("username", "password")(AuthenticationConfig.apply)
}

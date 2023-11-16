package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

trait AuthenticationConfigImplicits {
  implicit val configReaderAuthenticationConfig: ConfigReader[AuthenticationConfig] = ConfigReader.derived
}

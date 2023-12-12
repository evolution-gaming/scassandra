package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

trait AuthenticationConfigImplicits {
  implicit val configReaderAuthenticationConfig: ConfigReader[AuthenticationConfig] = deriveReader
}

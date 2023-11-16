package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

trait ReconnectionConfigImplicits {
  implicit val configReaderReconnectionConfig: ConfigReader[ReconnectionConfig] = ConfigReader.derived
}

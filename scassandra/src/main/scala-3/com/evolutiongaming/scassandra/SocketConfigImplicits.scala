package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

trait SocketConfigImplicits {
  implicit val configReaderSocketConfig: ConfigReader[SocketConfig] = ConfigReader.derived
}

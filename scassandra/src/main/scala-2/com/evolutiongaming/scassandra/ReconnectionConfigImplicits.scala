package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

trait ReconnectionConfigImplicits {
  implicit val configReaderReconnectionConfig: ConfigReader[ReconnectionConfig] = deriveReader
}

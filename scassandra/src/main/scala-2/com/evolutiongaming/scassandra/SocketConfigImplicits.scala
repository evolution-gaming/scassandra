package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

trait SocketConfigImplicits {
  implicit val configReaderSocketConfig: ConfigReader[SocketConfig] = deriveReader
}

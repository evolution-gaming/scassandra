package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import com.evolutiongaming.scassandra.ReplicationStrategyConfig.*

trait ReplicationStrategyConfigImplicits {
  implicit val configReaderSimple: ConfigReader[Simple] = deriveReader
}

package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import com.evolutiongaming.scassandra.ReplicationStrategyConfig.*

trait ReplicationStrategyConfigImplicits {
  implicit val configReaderSimple: ConfigReader[Simple] = ConfigReader.derived
}

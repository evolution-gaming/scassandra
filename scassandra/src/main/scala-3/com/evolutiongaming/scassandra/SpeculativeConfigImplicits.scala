package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

trait SpeculativeConfigImplicits {
  implicit val configReaderSpeculativeExecutionConfig: ConfigReader[SpeculativeExecutionConfig] = ConfigReader.derived
}

package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

trait SpeculativeConfigImplicits {
  implicit val configReaderSpeculativeExecutionConfig: ConfigReader[SpeculativeExecutionConfig] = deriveReader
}

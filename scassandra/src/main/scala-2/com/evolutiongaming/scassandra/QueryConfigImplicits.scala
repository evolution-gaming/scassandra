package com.evolutiongaming.scassandra

import com.datastax.driver.core.ConsistencyLevel
import com.evolutiongaming.scassandra.util.ConfigReaderFromEnum
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

trait QueryConfigImplicits {
  implicit val configReaderConsistencyLevel: ConfigReader[ConsistencyLevel] = ConfigReaderFromEnum(ConsistencyLevel.values())

  implicit val configReaderQueryConfig: ConfigReader[QueryConfig] = deriveReader
}

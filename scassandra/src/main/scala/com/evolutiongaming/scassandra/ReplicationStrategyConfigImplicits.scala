package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import com.evolutiongaming.scassandra.ReplicationStrategyConfig.*

trait ReplicationStrategyConfigImplicits {
  implicit val configReaderSimple: ConfigReader[Simple] = 
    ConfigReader.forProduct1[Simple, Option[Int]]("replication-factor") { replicationFactor => 
      val defaultConfig = Simple()

      Simple(replicationFactor.getOrElse(defaultConfig.replicationFactor))
    }
}

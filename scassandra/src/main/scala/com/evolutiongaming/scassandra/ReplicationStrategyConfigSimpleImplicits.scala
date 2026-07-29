package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.ReplicationStrategyConfig.*
import com.evolutiongaming.scassandra.util.PureconfigSyntax.*
import pureconfig.ConfigReader

trait ReplicationStrategyConfigSimpleImplicits {
  implicit val configReaderSimple: ConfigReader[Simple] = ConfigReader.fromCursor[Simple] { cursor =>
    val defaultConfig = Simple()

    for {
      objCur <- cursor.asObjectCursor
      replicationFactor <- objCur.getAtOpt[Int]("replication-factor")
        .map(_.getOrElse(defaultConfig.replicationFactor))
    } yield Simple(
      replicationFactor = replicationFactor,
    )
  }
}

package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.util.PureconfigSyntax.*
import pureconfig.ConfigReader

trait LoadBalancingConfigImplicits {
  implicit val configReaderLoadBalancingConfig: ConfigReader[LoadBalancingConfig] =
    ConfigReader.fromCursor[LoadBalancingConfig] { cursor =>
      val defaultConfig = LoadBalancingConfig()

      for {
        objCur <- cursor.asObjectCursor
        localDc <- objCur.getAtOpt[String]("local-dc").map(_.getOrElse(defaultConfig.localDc))
      } yield LoadBalancingConfig(localDc = localDc)
    }

}

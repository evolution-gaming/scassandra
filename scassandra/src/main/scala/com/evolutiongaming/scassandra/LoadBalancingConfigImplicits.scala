package com.evolutiongaming.scassandra

import pureconfig.ConfigReader

trait LoadBalancingConfigImplicits {
  implicit val configReaderLoadBalancingConfig: ConfigReader[LoadBalancingConfig] = 
    ConfigReader.forProduct2[LoadBalancingConfig, Option[String], Option[Boolean]]("local-dc", "allow-remote-dcs-for-local-consistency-level") { 
      (localDc, allowRemoteDcsForLocalConsistencyLevel) => 
        val defaultConfig = LoadBalancingConfig()

        LoadBalancingConfig(
          localDc.getOrElse(defaultConfig.localDc), 
          allowRemoteDcsForLocalConsistencyLevel.getOrElse(defaultConfig.allowRemoteDcsForLocalConsistencyLevel)
        )
    }
}
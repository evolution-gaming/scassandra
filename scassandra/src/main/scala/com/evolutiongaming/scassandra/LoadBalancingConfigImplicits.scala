package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.util.PureconfigSyntax._
import pureconfig.ConfigReader

trait LoadBalancingConfigImplicits {
  implicit val configReaderLoadBalancingConfig: ConfigReader[LoadBalancingConfig] = ConfigReader.fromCursor[LoadBalancingConfig] { cursor =>
    val defaultConfig = LoadBalancingConfig()
     
    for {
      objCur <- cursor.asObjectCursor
      localDc <- objCur.getAtOpt[String]("local-dc").map(_.getOrElse(defaultConfig.localDc))
      allowRemoteDcsForLocalConsistencyLevel <- objCur.getAtOpt[Boolean]("allow-remote-dcs-for-local-consistency-level").map(_.getOrElse(defaultConfig.allowRemoteDcsForLocalConsistencyLevel))
    } yield LoadBalancingConfig(
      localDc = localDc,
      allowRemoteDcsForLocalConsistencyLevel = allowRemoteDcsForLocalConsistencyLevel
    )  
  }

}
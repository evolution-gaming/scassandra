package com.evolutiongaming.scassandra

import com.datastax.driver.core.policies.{DCAwareRoundRobinPolicy, LoadBalancingPolicy, TokenAwarePolicy}
import com.evolutiongaming.config.ConfigHelper._
import com.typesafe.config.Config

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/load_balancing/]]
  */
final case class LoadBalancingConfig(
  localDc: String = "localDc",
  allowRemoteDcsForLocalConsistencyLevel: Boolean = false) {

  def asJava: Option[LoadBalancingPolicy] = {
    if (localDc.nonEmpty) {
      val policy = DCAwareRoundRobinPolicy.builder
        .withLocalDc(localDc)
        .build()
      val tokenAwarePolicy = new TokenAwarePolicy(policy)
      Some(tokenAwarePolicy)
    } else {
      None
    }
  }
}

object LoadBalancingConfig {

  val Default: LoadBalancingConfig = LoadBalancingConfig()

  
  def apply(config: Config): LoadBalancingConfig = apply(config, Default)

  def apply(config: Config, default: => LoadBalancingConfig): LoadBalancingConfig = {

    def get[A: FromConf](name: String) = config.getOpt[A](name)

    LoadBalancingConfig(
      localDc = get[String]("local-dc") getOrElse default.localDc,
      allowRemoteDcsForLocalConsistencyLevel = get[Boolean]("allow-remote-dcs-for-local-consistency-level") getOrElse default.allowRemoteDcsForLocalConsistencyLevel)
  }
}

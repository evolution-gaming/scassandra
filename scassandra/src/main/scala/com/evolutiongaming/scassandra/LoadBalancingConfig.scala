package com.evolutiongaming.scassandra

import com.datastax.driver.core.policies.{DCAwareRoundRobinPolicy, LoadBalancingPolicy, TokenAwarePolicy}
import com.datastax.oss.driver.api.core.loadbalancing.LoadBalancingPolicy
import com.typesafe.config.Config
import pureconfig.ConfigSource

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/load_balancing/]]
  */
final case class LoadBalancingConfig(
  localDc: String = "localDc",
  allowRemoteDcsForLocalConsistencyLevel: Boolean = false,
) {

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

object LoadBalancingConfig extends LoadBalancingConfigImplicits {

  val Default: LoadBalancingConfig = LoadBalancingConfig()

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): LoadBalancingConfig = apply(config, Default)

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config, default: => LoadBalancingConfig): LoadBalancingConfig = fromConfig(config, default)

  def fromConfig(config: Config, default: => LoadBalancingConfig): LoadBalancingConfig = {
    ConfigSource.fromConfig(config).load[LoadBalancingConfig] getOrElse default
  }
}

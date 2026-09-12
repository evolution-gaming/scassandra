package com.evolutiongaming.scassandra

import com.typesafe.config.Config
import pureconfig.ConfigSource

/**
 * See `basic.load-balancing-policy` in the driver reference configuration.
 *
 * @param localDc
 *   the local datacenter, when empty it is inferred from the contact points
 */
final case class LoadBalancingConfig(localDc: String = "localDc")

object LoadBalancingConfig extends LoadBalancingConfigImplicits {

  val Default: LoadBalancingConfig = LoadBalancingConfig()

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): LoadBalancingConfig = apply(config, Default)

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config, default: => LoadBalancingConfig): LoadBalancingConfig =
    fromConfig(config, default)

  def fromConfig(config: Config, default: => LoadBalancingConfig): LoadBalancingConfig = {
    ConfigSource.fromConfig(config).load[LoadBalancingConfig] getOrElse default
  }
}

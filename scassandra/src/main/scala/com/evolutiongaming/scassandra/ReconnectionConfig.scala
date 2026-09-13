package com.evolutiongaming.scassandra

import com.typesafe.config.Config
import pureconfig.ConfigSource

import scala.concurrent.duration.*

/**
 * Exponential reconnection policy, see `advanced.reconnection-policy` in the driver
 * reference configuration.
 */
final case class ReconnectionConfig(
  minDelay: FiniteDuration = 1.second,
  maxDelay: FiniteDuration = 10.minutes,
)

object ReconnectionConfig extends ReconnectionConfigImplicits {

  val Default: ReconnectionConfig = ReconnectionConfig()

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): ReconnectionConfig = apply(config, Default)

  @deprecated("use ConfigReader instead", "1.2.0")
  def apply(config: Config, default: => ReconnectionConfig): ReconnectionConfig = fromConfig(config, default)

  def fromConfig(config: Config, default: => ReconnectionConfig): ReconnectionConfig = {
    ConfigSource.fromConfig(config).load[ReconnectionConfig] getOrElse default
  }
}

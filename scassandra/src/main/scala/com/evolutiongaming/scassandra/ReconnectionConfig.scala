package com.evolutiongaming.scassandra

import com.datastax.driver.core.policies.{ExponentialReconnectionPolicy, ReconnectionPolicy}
import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.connection.ReconnectionPolicy
import com.datastax.oss.driver.internal.core.connection.ExponentialReconnectionPolicy
import com.evolutiongaming.scassandra.util.FakeConfig
import com.typesafe.config.Config
import pureconfig.ConfigSource

import scala.concurrent.duration.*

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/reconnection/]]
  */
final case class ReconnectionConfig(
  minDelay: FiniteDuration = 1.second,
  maxDelay: FiniteDuration = 10.minutes,
) {

  def asJava: ReconnectionPolicy = {
    val config = FakeConfig.createFakeContext(
      DefaultDriverOption.RECONNECTION_BASE_DELAY.getPath -> java.time.Duration.ofMillis(minDelay.toMillis),
      DefaultDriverOption.RECONNECTION_MAX_DELAY.getPath -> java.time.Duration.ofMillis(maxDelay.toMillis),
    )
    new ExponentialReconnectionPolicy(config)
  }
}

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

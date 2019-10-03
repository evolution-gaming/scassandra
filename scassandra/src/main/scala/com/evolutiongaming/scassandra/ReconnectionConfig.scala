package com.evolutiongaming.scassandra

import com.datastax.driver.core.policies.{ExponentialReconnectionPolicy, ReconnectionPolicy}
import com.typesafe.config.Config
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader

import scala.concurrent.duration._

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/reconnection/]]
  */
final case class ReconnectionConfig(
  minDelay: FiniteDuration = 1.second,
  maxDelay: FiniteDuration = 10.minutes) {

  def asJava: ReconnectionPolicy = {
    new ExponentialReconnectionPolicy(minDelay.toMillis, maxDelay.toMillis)
  }
}

object ReconnectionConfig {

  val Default: ReconnectionConfig = ReconnectionConfig()

  implicit val configReaderReconnectionConfig: ConfigReader[ReconnectionConfig] = deriveReader

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): ReconnectionConfig = apply(config, Default)

  @deprecated("use ConfigReader instead", "1.2.0")
  def apply(config: Config, default: => ReconnectionConfig): ReconnectionConfig = fromConfig(config, default)


  def fromConfig(config: Config, default: => ReconnectionConfig): ReconnectionConfig = {
    ConfigSource.fromConfig(config).load[ReconnectionConfig] getOrElse default
  }
}

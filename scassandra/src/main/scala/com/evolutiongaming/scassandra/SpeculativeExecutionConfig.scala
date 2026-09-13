package com.evolutiongaming.scassandra

import com.typesafe.config.Config
import pureconfig.ConfigSource

import scala.concurrent.duration.*

/**
 * Constant speculative execution policy, see `advanced.speculative-execution-policy` in
 * the driver reference configuration.
 *
 * @param maxExecutions
 *   number of speculative executions in addition to the initial one
 */
final case class SpeculativeExecutionConfig(
  delay: FiniteDuration = 500.millis,
  maxExecutions: Int = 2,
)

object SpeculativeExecutionConfig extends SpeculativeConfigImplicits {

  val Default: SpeculativeExecutionConfig = SpeculativeExecutionConfig()

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): SpeculativeExecutionConfig = fromConfig(config, Default)

  @deprecated("use ConfigReader instead", "1.2.0")
  def apply(config: Config, default: => SpeculativeExecutionConfig): SpeculativeExecutionConfig = {
    fromConfig(config, default)
  }

  def fromConfig(config: Config, default: => SpeculativeExecutionConfig): SpeculativeExecutionConfig = {
    ConfigSource.fromConfig(config).load[SpeculativeExecutionConfig] getOrElse default
  }
}

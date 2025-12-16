package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.specex.SpeculativeExecutionPolicy
import com.datastax.oss.driver.internal.core.specex.ConstantSpeculativeExecutionPolicy
import com.evolutiongaming.scassandra.util.FakeConfig
import com.typesafe.config.Config
import pureconfig.ConfigSource

import java.time.Duration as JDuration
import scala.concurrent.duration.*

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/speculative_execution/]]
  */
final case class SpeculativeExecutionConfig(
  delay: FiniteDuration = 500.millis,
  maxExecutions: Int = 2) {

  def asJava: SpeculativeExecutionPolicy = {
    val config = FakeConfig.createFakeContext(
      DefaultDriverOption.SPECULATIVE_EXECUTION_MAX.getPath -> maxExecutions,
      DefaultDriverOption.SPECULATIVE_EXECUTION_DELAY.getPath -> JDuration.ofMillis(delay.toMillis),
    )
    new ConstantSpeculativeExecutionPolicy(config, "")
  }
}

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

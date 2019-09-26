package com.evolutiongaming.scassandra

import com.datastax.driver.core.policies.{ConstantSpeculativeExecutionPolicy, SpeculativeExecutionPolicy}
import com.typesafe.config.Config
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

import scala.concurrent.duration._

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/speculative_execution/]]
  */
final case class SpeculativeExecutionConfig(
  delay: FiniteDuration = 500.millis,
  maxExecutions: Int = 2) {

  def asJava: SpeculativeExecutionPolicy = {
    new ConstantSpeculativeExecutionPolicy(delay.toMillis, maxExecutions)
  }
}

object SpeculativeExecutionConfig {

  val Default: SpeculativeExecutionConfig = SpeculativeExecutionConfig()

  implicit val configReaderSpeculativeExecutionConfig: ConfigReader[SpeculativeExecutionConfig] = deriveReader


  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): SpeculativeExecutionConfig = apply(config, Default)

  def apply(config: Config, default: => SpeculativeExecutionConfig): SpeculativeExecutionConfig = {
    ConfigSource.fromConfig(config).load[SpeculativeExecutionConfig] getOrElse default
  }
}

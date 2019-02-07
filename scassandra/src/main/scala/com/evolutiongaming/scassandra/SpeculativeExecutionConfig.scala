package com.evolutiongaming.scassandra

import com.datastax.driver.core.policies.{ConstantSpeculativeExecutionPolicy, SpeculativeExecutionPolicy}
import com.evolutiongaming.config.ConfigHelper._
import com.typesafe.config.Config

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


  def apply(config: Config): SpeculativeExecutionConfig = apply(config, Default)

  def apply(config: Config, default: => SpeculativeExecutionConfig): SpeculativeExecutionConfig = {
    def get[A: FromConf](name: String) = config.getOpt[A](name)

    SpeculativeExecutionConfig(
      delay = get[FiniteDuration]("delay") getOrElse default.delay,
      maxExecutions = get[Int]("max-executions") getOrElse default.maxExecutions)
  }
}

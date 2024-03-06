package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import scala.concurrent.duration.FiniteDuration

trait SpeculativeConfigImplicits {
  implicit val configReaderSpeculativeExecutionConfig: ConfigReader[SpeculativeExecutionConfig] = 
    ConfigReader.forProduct2[SpeculativeExecutionConfig, Option[FiniteDuration], Option[Int]]("delay", "max-executions") { 
      (delay, maxExecutions) => 
        val defaultConfig = SpeculativeExecutionConfig()

        SpeculativeExecutionConfig(
          delay.getOrElse(defaultConfig.delay), 
          maxExecutions.getOrElse(defaultConfig.maxExecutions)
        )
    }
}

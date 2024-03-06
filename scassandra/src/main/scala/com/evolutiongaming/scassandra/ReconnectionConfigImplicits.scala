package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import scala.concurrent.duration.FiniteDuration

trait ReconnectionConfigImplicits {
  implicit val configReaderReconnectionConfig: ConfigReader[ReconnectionConfig] = 
    ConfigReader.forProduct2[ReconnectionConfig, Option[FiniteDuration], Option[FiniteDuration]]("min-delay", "max-delay") { 
      (minDelay, maxDelay) => 
        val defaultConfig = ReconnectionConfig()

        ReconnectionConfig(
          minDelay.getOrElse(defaultConfig.minDelay), 
          maxDelay.getOrElse(defaultConfig.maxDelay)
        )
      }
}

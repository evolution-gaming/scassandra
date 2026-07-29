package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.util.PureconfigSyntax.*
import pureconfig.ConfigReader

import scala.concurrent.duration.FiniteDuration

trait ReconnectionConfigImplicits {
  implicit val configReaderReconnectionConfig: ConfigReader[ReconnectionConfig] =
    ConfigReader.fromCursor[ReconnectionConfig] { cursor =>
      val defaultConfig = ReconnectionConfig()

      for {
        objCur <- cursor.asObjectCursor
        minDelay <- objCur.getAtOpt[FiniteDuration]("min-delay").map(_.getOrElse(defaultConfig.minDelay))
        maxDelay <- objCur.getAtOpt[FiniteDuration]("max-delay").map(_.getOrElse(defaultConfig.maxDelay))
      } yield ReconnectionConfig(
        minDelay = minDelay,
        maxDelay = maxDelay,
      )
    }

}

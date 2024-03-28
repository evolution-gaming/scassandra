package com.evolutiongaming.scassandra

import scala.concurrent.duration.FiniteDuration
import com.evolutiongaming.scassandra.util.PureconfigSyntax._
import pureconfig.ConfigReader

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
        maxDelay = maxDelay
      )
    }

}

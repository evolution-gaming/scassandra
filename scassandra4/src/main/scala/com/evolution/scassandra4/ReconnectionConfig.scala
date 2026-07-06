package com.evolution.scassandra4

import com.evolution.scassandra4.util.PureconfigSyntax._
import pureconfig.ConfigReader

import scala.concurrent.duration._

/**
  * Translated to driver 4's `ExponentialReconnectionPolicy`
  * (`advanced.reconnection-policy`), see [[CreateDriverConfigLoader]].
  */
final case class ReconnectionConfig(
  minDelay: FiniteDuration = 1.second,
  maxDelay: FiniteDuration = 10.minutes)

object ReconnectionConfig {

  val Default: ReconnectionConfig = ReconnectionConfig()

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

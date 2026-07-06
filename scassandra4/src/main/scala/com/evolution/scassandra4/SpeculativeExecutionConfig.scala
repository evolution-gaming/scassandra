package com.evolution.scassandra4

import com.evolution.scassandra4.util.PureconfigSyntax._
import pureconfig.ConfigReader

import scala.concurrent.duration._

/**
  * Translated to driver 4's `ConstantSpeculativeExecutionPolicy`
  * (`advanced.speculative-execution-policy`), see [[CreateDriverConfigLoader]].
  *
  * `maxExecutions` keeps the driver 3 meaning: the number of additional,
  * speculative executions (driver 4 counts the initial execution as well and
  * the translation accounts for that).
  */
final case class SpeculativeExecutionConfig(
  delay: FiniteDuration = 500.millis,
  maxExecutions: Int = 2)

object SpeculativeExecutionConfig {

  val Default: SpeculativeExecutionConfig = SpeculativeExecutionConfig()

  implicit val configReaderSpeculativeExecutionConfig: ConfigReader[SpeculativeExecutionConfig] =
    ConfigReader.fromCursor[SpeculativeExecutionConfig] { cursor =>
      val defaultConfig = SpeculativeExecutionConfig()

      for {
        objCur <- cursor.asObjectCursor
        delay <- objCur.getAtOpt[FiniteDuration]("delay").map(_.getOrElse(defaultConfig.delay))
        maxExecutions <- objCur.getAtOpt[Int]("max-executions").map(_.getOrElse(defaultConfig.maxExecutions))
      } yield SpeculativeExecutionConfig(
        delay = delay,
        maxExecutions = maxExecutions
      )
    }
}

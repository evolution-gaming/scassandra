package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.util.PureconfigSyntax.*
import pureconfig.ConfigReader

import scala.concurrent.duration.FiniteDuration

trait SpeculativeConfigImplicits {
  implicit val configReaderSpeculativeExecutionConfig: ConfigReader[SpeculativeExecutionConfig] =
    ConfigReader.fromCursor[SpeculativeExecutionConfig] { cursor =>
      val defaultConfig = SpeculativeExecutionConfig()

      for {
        objCur <- cursor.asObjectCursor
        delay <- objCur.getAtOpt[FiniteDuration]("delay").map(_.getOrElse(defaultConfig.delay))
        maxExecutions <- objCur.getAtOpt[Int]("max-executions").map(_.getOrElse(defaultConfig.maxExecutions))
      } yield SpeculativeExecutionConfig(
        delay = delay,
        maxExecutions = maxExecutions,
      )
    }
}

package com.evolutiongaming.scassandra

import scala.concurrent.duration.FiniteDuration
import com.evolutiongaming.scassandra.util.PureconfigSyntax._
import pureconfig.ConfigReader

trait SpeculativeConfigImplicits {
  implicit val configReaderSpeculativeExecutionConfig: ConfigReader[SpeculativeExecutionConfig] =  ConfigReader.fromCursor[SpeculativeExecutionConfig] { cursor =>
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

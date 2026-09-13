package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.util.PureconfigSyntax.*
import com.typesafe.config.Config
import pureconfig.{ConfigReader, ConfigSource}

import scala.concurrent.duration.*

/**
 * Connection pools have a fixed size, see `advanced.connection` and `advanced.heartbeat`
 * in the driver reference configuration.
 */
final case class PoolingConfig(
  localSize: Int = 1,
  remoteSize: Int = 1,
  maxRequestsPerConnection: Int = 1024,
  heartbeatInterval: FiniteDuration = 30.seconds,
)

object PoolingConfig {

  val Default: PoolingConfig = PoolingConfig()

  implicit val configReaderPoolingConfig: ConfigReader[PoolingConfig] =
    ConfigReader.fromCursor[PoolingConfig] { cursor =>
      for {
        objCur <- cursor.asObjectCursor
        localSize <- objCur.getAtOpt[Int]("local-size").map(_.getOrElse(Default.localSize))
        remoteSize <- objCur.getAtOpt[Int]("remote-size").map(_.getOrElse(Default.remoteSize))
        maxRequestsPerConnection <- objCur.getAtOpt[Int]("max-requests-per-connection")
          .map(_.getOrElse(Default.maxRequestsPerConnection))
        heartbeatInterval <- objCur.getAtOpt[FiniteDuration]("heartbeat-interval")
          .map(_.getOrElse(Default.heartbeatInterval))
      } yield PoolingConfig(
        localSize = localSize,
        remoteSize = remoteSize,
        maxRequestsPerConnection = maxRequestsPerConnection,
        heartbeatInterval = heartbeatInterval,
      )
    }

  @deprecated("use ConfigReader instead", "1.2.0")
  def apply(config: Config): PoolingConfig = fromConfig(config, Default)

  @deprecated("use ConfigReader instead", "1.2.0")
  def apply(config: Config, default: => PoolingConfig): PoolingConfig = fromConfig(config, default)

  def fromConfig(config: Config, default: => PoolingConfig): PoolingConfig = {
    ConfigSource.fromConfig(config).load[PoolingConfig] getOrElse default
  }
}

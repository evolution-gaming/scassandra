package com.evolution.scassandra4

import com.typesafe.config.Config
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

import scala.concurrent.duration._

/** Connection pooling, keeping the driver 3 era schema.
  *
  * Driver 4 pools have a fixed size and no borrowing, so only a subset
  * translates, see [[CreateDriverConfigLoader]]:
  *   - `local.connectionsPerHostMax` → `advanced.connection.pool.local.size`
  *   - `remote.connectionsPerHostMax` → `advanced.connection.pool.remote.size`
  *   - `local.maxRequestsPerConnection` → `advanced.connection.max-requests-per-connection`
  *     (driver 4 has a single setting for local and remote)
  *   - `heartbeatInterval` → `advanced.heartbeat.interval`
  *
  * Ignored (no driver 4 counterpart): `poolTimeout`, `idleTimeout`,
  * `maxQueueSize`, `newConnectionThreshold`, `connectionsPerHostMin`,
  * `remote.maxRequestsPerConnection`.
  */
final case class PoolingConfig(
  local: PoolingConfig.HostConfig = PoolingConfig.HostConfig.Local,
  remote: PoolingConfig.HostConfig = PoolingConfig.HostConfig.Remote,
  poolTimeout: FiniteDuration = 5.seconds,
  idleTimeout: FiniteDuration = 2.minutes,
  maxQueueSize: Int = 256,
  heartbeatInterval: FiniteDuration = 30.seconds)

object PoolingConfig {

  val Default: PoolingConfig = PoolingConfig()

  implicit val configReaderPoolingConfig: ConfigReader[PoolingConfig] = {
    (cursor: ConfigCursor) => {
      for {
        cursor <- cursor.asObjectCursor
      } yield {
        fromConfig(cursor.objValue.toConfig, Default)
      }
    }
  }


  def fromConfig(config: Config, default: => PoolingConfig): PoolingConfig = {

    val source = ConfigSource.fromConfig(config)

    def group(name: String, default: HostConfig) = {
      source.at(name).load[Config].fold(_ => default, config => HostConfig.fromConfig(config, default))
    }

    def get[A: ConfigReader](name: String) = source.at(name).load[A]

    PoolingConfig(
      local = group("local", default.local),
      remote = group("remote", default.remote),
      poolTimeout = get[FiniteDuration]("pool-timeout") getOrElse default.poolTimeout,
      idleTimeout = get[FiniteDuration]("idle-timeout") getOrElse default.idleTimeout,
      maxQueueSize = get[Int]("max-queue-size") getOrElse default.maxQueueSize,
      heartbeatInterval = get[FiniteDuration]("heartbeat-interval") getOrElse default.heartbeatInterval)
  }


  final case class HostConfig(
    newConnectionThreshold: Int,
    maxRequestsPerConnection: Int,
    connectionsPerHostMin: Int,
    connectionsPerHostMax: Int)

  object HostConfig {

    val Local: HostConfig = HostConfig(
      newConnectionThreshold = 800,
      maxRequestsPerConnection = 32768,
      connectionsPerHostMin = 1,
      connectionsPerHostMax = 4)

    val Remote: HostConfig = HostConfig(
      newConnectionThreshold = 200,
      maxRequestsPerConnection = 2000,
      connectionsPerHostMin = 1,
      connectionsPerHostMax = 4)


    def fromConfig(config: Config, default: => HostConfig): HostConfig = {

      val source = ConfigSource.fromConfig(config)

      def get[A: ConfigReader](name: String) = source.at(name).load[A]

      HostConfig(
        newConnectionThreshold = get[Int]("new-connection-threshold") getOrElse default.newConnectionThreshold,
        maxRequestsPerConnection = get[Int]("max-requests-per-connection") getOrElse default.maxRequestsPerConnection,
        connectionsPerHostMin = get[Int]("connections-per-host-min") getOrElse default.connectionsPerHostMin,
        connectionsPerHostMax = get[Int]("connections-per-host-max") getOrElse default.connectionsPerHostMax)
    }
  }
}

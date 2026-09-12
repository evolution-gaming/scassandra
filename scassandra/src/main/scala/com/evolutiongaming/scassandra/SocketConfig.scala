package com.evolutiongaming.scassandra

import com.typesafe.config.Config
import pureconfig.ConfigSource

import scala.concurrent.duration.*

/**
 * See `advanced.socket`, `advanced.connection.connect-timeout` and
 * `basic.request.timeout` in the driver reference configuration, `readTimeout` is the
 * request timeout.
 */
final case class SocketConfig(
  connectTimeout: FiniteDuration = 5.seconds,
  readTimeout: FiniteDuration = 12.seconds,
  keepAlive: Option[Boolean] = None,
  reuseAddress: Option[Boolean] = None,
  soLinger: Option[Int] = None,
  tcpNoDelay: Option[Boolean] = Some(true),
  receiveBufferSize: Option[Int] = None,
  sendBufferSize: Option[Int] = None,
)

object SocketConfig extends SocketConfigImplicits {

  val Default: SocketConfig = SocketConfig()

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): SocketConfig = fromConfig(config, Default)

  @deprecated("use ConfigReader instead", "1.2.0")
  def apply(config: Config, default: => SocketConfig): SocketConfig = fromConfig(config, default)

  def fromConfig(config: Config, default: => SocketConfig): SocketConfig = {
    ConfigSource.fromConfig(config).load[SocketConfig] getOrElse default
  }
}

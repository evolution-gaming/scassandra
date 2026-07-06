package com.evolution.scassandra4

import com.evolution.scassandra4.util.PureconfigSyntax._
import pureconfig.ConfigReader

import scala.concurrent.duration._

/** Socket options, keeping the driver 3 era schema.
  *
  * See [[CreateDriverConfigLoader]] for the translation; note that
  * `readTimeout` maps to driver 4's per-request timeout
  * (`basic.request.timeout`), the closest driver 4 counterpart of driver 3's
  * socket read timeout.
  */
final case class SocketConfig(
  connectTimeout: FiniteDuration = 5.seconds,
  readTimeout: FiniteDuration = 12.seconds,
  keepAlive: Option[Boolean] = None,
  reuseAddress: Option[Boolean] = None,
  soLinger: Option[Int] = None,
  tcpNoDelay: Option[Boolean] = Some(true),
  receiveBufferSize: Option[Int] = None,
  sendBufferSize: Option[Int] = None)

object SocketConfig {

  val Default: SocketConfig = SocketConfig()

  implicit val configReaderSocketConfig: ConfigReader[SocketConfig] =
    ConfigReader.fromCursor[SocketConfig] { cursor =>
      val defaultConfig = SocketConfig()

      for {
        objCur <- cursor.asObjectCursor
        connectTimeout <- objCur.getAtOpt[FiniteDuration]("connect-timeout").map(_.getOrElse(defaultConfig.connectTimeout))
        readTimeout <- objCur.getAtOpt[FiniteDuration]("read-timeout").map(_.getOrElse(defaultConfig.readTimeout))
        keepAlive <- objCur.getAtOpt[Boolean]("keep-alive").map(_.orElse(defaultConfig.keepAlive))
        reuseAddress <- objCur.getAtOpt[Boolean]("reuse-address").map(_.orElse(defaultConfig.reuseAddress))
        soLinger <- objCur.getAtOpt[Int]("so-linger").map(_.orElse(defaultConfig.soLinger))
        tcpNoDelay <- objCur.getAtOpt[Boolean]("tcp-no-delay").map(_.orElse(defaultConfig.tcpNoDelay))
        receiveBufferSize <- objCur.getAtOpt[Int]("receive-buffer-size").map(_.orElse(defaultConfig.receiveBufferSize))
        sendBufferSize <- objCur.getAtOpt[Int]("send-buffer-size").map(_.orElse(defaultConfig.sendBufferSize))
      } yield SocketConfig(
        connectTimeout = connectTimeout,
        readTimeout = readTimeout,
        keepAlive = keepAlive,
        reuseAddress = reuseAddress,
        soLinger = soLinger,
        tcpNoDelay = tcpNoDelay,
        receiveBufferSize = receiveBufferSize,
        sendBufferSize = sendBufferSize
      )
    }
}

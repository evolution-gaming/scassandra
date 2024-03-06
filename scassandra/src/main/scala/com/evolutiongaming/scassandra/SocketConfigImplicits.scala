package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import scala.concurrent.duration.FiniteDuration

trait SocketConfigImplicits {
  implicit val configReaderSocketConfig: ConfigReader[SocketConfig] = 
    ConfigReader.forProduct8[
      SocketConfig, 
      Option[FiniteDuration],
      Option[FiniteDuration],
      Option[Boolean],
      Option[Boolean],
      Option[Int],
      Option[Boolean],
      Option[Int],
      Option[Int]
    ](
      "connect-timeout",
      "read-timeout",
      "keep-alive",
      "reuse-address",
      "so-linger",
      "tcp-no-delay",
      "receive-buffer-size",
      "send-buffer-size",
    ) { (connectTimeout, readTimeout, keepAlive, reuseAddress, soLinger, tcpNoDelay, receiveBufferSize, sendBufferSize) => 
      val defaultConfig = SocketConfig()

      SocketConfig(
        connectTimeout.getOrElse(defaultConfig.connectTimeout),
        readTimeout.getOrElse(defaultConfig.readTimeout),
        keepAlive.orElse(defaultConfig.keepAlive),
        reuseAddress.orElse(defaultConfig.reuseAddress),
        soLinger.orElse(defaultConfig.soLinger),
        tcpNoDelay.orElse(defaultConfig.tcpNoDelay),
        receiveBufferSize.orElse(defaultConfig.receiveBufferSize),
        sendBufferSize.orElse(defaultConfig.sendBufferSize)
      )
      }
}

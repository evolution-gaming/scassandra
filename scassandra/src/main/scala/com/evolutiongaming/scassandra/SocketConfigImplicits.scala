package com.evolutiongaming.scassandra

import scala.concurrent.duration.FiniteDuration
import com.evolutiongaming.scassandra.util.PureconfigSyntax._
import pureconfig.ConfigReader

trait SocketConfigImplicits {
  implicit val configReaderSocketConfig: ConfigReader[SocketConfig] = ConfigReader.fromCursor[SocketConfig] { cursor =>
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
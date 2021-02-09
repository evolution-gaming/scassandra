package com.evolutiongaming.scassandra

import cats.syntax.all._
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource

import scala.concurrent.duration._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SocketConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[SocketConfig] shouldEqual SocketConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("socket.conf"))
    val expected = SocketConfig(
      connectTimeout = 1.millis,
      readTimeout = 2.seconds,
      keepAlive = Some(false),
      reuseAddress = Some(false),
      soLinger = Some(3),
      tcpNoDelay = Some(false),
      receiveBufferSize = Some(4),
      sendBufferSize = Some(5))
    ConfigSource.fromConfig(config).load[SocketConfig] shouldEqual expected.asRight
  }
}

package com.evolutiongaming.scassandra

import cats.implicits.*
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.annotation.nowarn
import scala.concurrent.duration.*

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
      sendBufferSize = Some(5),
    )
    ConfigSource.fromConfig(config).load[SocketConfig] shouldEqual expected.asRight
  }

  test("asJava") {
    val options = SocketConfig(
      connectTimeout = 1.second,
      readTimeout = 2.seconds,
      keepAlive = Some(true),
      reuseAddress = Some(true),
      soLinger = Some(3),
      tcpNoDelay = Some(false),
      receiveBufferSize = Some(4),
      sendBufferSize = Some(5),
    ).asJava
    options.getConnectTimeoutMillis shouldEqual 1000
    options.getReadTimeoutMillis shouldEqual 2000
    options.getKeepAlive shouldEqual true
    options.getReuseAddress shouldEqual true
    options.getSoLinger shouldEqual 3
    options.getTcpNoDelay shouldEqual false
    options.getReceiveBufferSize shouldEqual 4
    options.getSendBufferSize shouldEqual 5
  }

  test("asJava leaves unset options at driver defaults") {
    val options = SocketConfig.Default.asJava
    options.getConnectTimeoutMillis shouldEqual 5000
    options.getReadTimeoutMillis shouldEqual 12000
    options.getKeepAlive shouldBe null
    options.getReuseAddress shouldBe null
    options.getSoLinger shouldBe null
    options.getTcpNoDelay shouldEqual true
    options.getReceiveBufferSize shouldBe null
    options.getSendBufferSize shouldBe null
  }

  test("fromConfig falls back to default on invalid config") {
    val config = ConfigFactory.parseString("connect-timeout = nope")
    SocketConfig.fromConfig(config, SocketConfig.Default) shouldEqual SocketConfig.Default
  }

  test("deprecated apply") {
    val config = ConfigFactory.parseString("read-timeout = 3s")
    (SocketConfig(config): @nowarn("cat=deprecation")) shouldEqual SocketConfig(readTimeout = 3.seconds)
    (SocketConfig(config, SocketConfig.Default): @nowarn("cat=deprecation")) shouldEqual
      SocketConfig(readTimeout = 3.seconds)
  }
}

package com.evolutiongaming.scassandra

import cats.implicits.*
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.annotation.nowarn
import scala.concurrent.duration.*

class ReconnectionConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[ReconnectionConfig] shouldEqual ReconnectionConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("reconnection.conf"))
    val expected = ReconnectionConfig(
      minDelay = 1.millis,
      maxDelay = 2.seconds,
    )
    ConfigSource.fromConfig(config).load[ReconnectionConfig] shouldEqual expected.asRight
  }

  test("fromConfig falls back to default on invalid config") {
    val config = ConfigFactory.parseString("min-delay = nope")
    ReconnectionConfig.fromConfig(config, ReconnectionConfig.Default) shouldEqual ReconnectionConfig.Default
  }

  test("deprecated apply") {
    val config = ConfigFactory.parseString("max-delay = 3s")
    (ReconnectionConfig(config): @nowarn("cat=deprecation")) shouldEqual
      ReconnectionConfig(maxDelay = 3.seconds)
    (ReconnectionConfig(config, ReconnectionConfig.Default): @nowarn("cat=deprecation")) shouldEqual
      ReconnectionConfig(maxDelay = 3.seconds)
  }
}

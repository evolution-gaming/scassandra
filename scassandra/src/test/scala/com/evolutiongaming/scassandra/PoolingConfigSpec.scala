package com.evolutiongaming.scassandra

import cats.implicits.*
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.annotation.nowarn
import scala.concurrent.duration.*

class PoolingConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[PoolingConfig] shouldEqual PoolingConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("pooling.conf"))
    val expected = PoolingConfig(
      localSize = 2,
      remoteSize = 3,
      maxRequestsPerConnection = 4,
      heartbeatInterval = 5.hours,
    )
    ConfigSource.fromConfig(config).load[PoolingConfig] shouldEqual expected.asRight
  }

  test("fromConfig falls back to default on invalid config") {
    val config = ConfigFactory.parseString("local-size = nope")
    PoolingConfig.fromConfig(config, PoolingConfig.Default) shouldEqual PoolingConfig.Default
  }

  test("deprecated apply") {
    val config = ConfigFactory.parseString("local-size = 7")
    (PoolingConfig(config): @nowarn("cat=deprecation")) shouldEqual PoolingConfig(localSize = 7)
    (PoolingConfig(config, PoolingConfig.Default): @nowarn("cat=deprecation")) shouldEqual
      PoolingConfig(localSize = 7)
  }
}

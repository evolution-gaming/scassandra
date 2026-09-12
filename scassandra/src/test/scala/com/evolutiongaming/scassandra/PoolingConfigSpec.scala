package com.evolutiongaming.scassandra

import cats.implicits.*
import com.datastax.driver.core.HostDistance
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
      local = PoolingConfig.HostConfig(
        newConnectionThreshold = 1,
        maxRequestsPerConnection = 2,
        connectionsPerHostMin = 3,
        connectionsPerHostMax = 4,
      ),
      remote = PoolingConfig.HostConfig(
        newConnectionThreshold = 5,
        maxRequestsPerConnection = 6,
        connectionsPerHostMin = 7,
        connectionsPerHostMax = 8,
      ),
      poolTimeout = 1.millis,
      idleTimeout = 2.seconds,
      maxQueueSize = 3,
      heartbeatInterval = 4.hours,
    )
    ConfigSource.fromConfig(config).load[PoolingConfig] shouldEqual expected.asRight
  }

  test("asJava") {
    val options = PoolingConfig(
      local = PoolingConfig.HostConfig(
        newConnectionThreshold = 100,
        maxRequestsPerConnection = 1000,
        connectionsPerHostMin = 2,
        connectionsPerHostMax = 3,
      ),
      remote = PoolingConfig.HostConfig(
        newConnectionThreshold = 50,
        maxRequestsPerConnection = 500,
        connectionsPerHostMin = 1,
        connectionsPerHostMax = 2,
      ),
      poolTimeout = 1.second,
      idleTimeout = 2.minutes,
      maxQueueSize = 3,
      heartbeatInterval = 4.seconds,
    ).asJava
    options.getNewConnectionThreshold(HostDistance.LOCAL) shouldEqual 100
    options.getMaxRequestsPerConnection(HostDistance.LOCAL) shouldEqual 1000
    options.getCoreConnectionsPerHost(HostDistance.LOCAL) shouldEqual 2
    options.getMaxConnectionsPerHost(HostDistance.LOCAL) shouldEqual 3
    options.getNewConnectionThreshold(HostDistance.REMOTE) shouldEqual 50
    options.getMaxRequestsPerConnection(HostDistance.REMOTE) shouldEqual 500
    options.getCoreConnectionsPerHost(HostDistance.REMOTE) shouldEqual 1
    options.getMaxConnectionsPerHost(HostDistance.REMOTE) shouldEqual 2
    options.getPoolTimeoutMillis shouldEqual 1000
    options.getIdleTimeoutSeconds shouldEqual 120
    options.getMaxQueueSize shouldEqual 3
    options.getHeartbeatIntervalSeconds shouldEqual 4
  }

  test("fromConfig falls back per field") {
    val config = ConfigFactory.parseString("""
      local { connections-per-host-max = 9, new-connection-threshold = nope }
      remote = nope
      pool-timeout = nope
      max-queue-size = 7
    """)
    val expected = PoolingConfig.Default.copy(
      local = PoolingConfig.HostConfig.Local.copy(connectionsPerHostMax = 9),
      maxQueueSize = 7,
    )
    PoolingConfig.fromConfig(config, PoolingConfig.Default) shouldEqual expected
  }

  test("deprecated apply") {
    val config = ConfigFactory.parseString("max-queue-size = 7")
    (PoolingConfig(config): @nowarn("cat=deprecation")) shouldEqual PoolingConfig(maxQueueSize = 7)
    (PoolingConfig(config, PoolingConfig.Default): @nowarn("cat=deprecation")) shouldEqual
      PoolingConfig(maxQueueSize = 7)
  }
}

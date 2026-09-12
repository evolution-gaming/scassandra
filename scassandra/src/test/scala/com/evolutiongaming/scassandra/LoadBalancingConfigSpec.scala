package com.evolutiongaming.scassandra

import cats.implicits.*
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.annotation.nowarn

class LoadBalancingConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[LoadBalancingConfig] shouldEqual LoadBalancingConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("load-balancing.conf"))
    val expected = LoadBalancingConfig(localDc = "local")
    ConfigSource.fromConfig(config).load[LoadBalancingConfig] shouldEqual expected.asRight
  }

  test("fromConfig falls back to default on invalid config") {
    val config = ConfigFactory.parseString("local-dc = [1]")
    LoadBalancingConfig.fromConfig(config, LoadBalancingConfig.Default) shouldEqual
      LoadBalancingConfig.Default
  }

  test("deprecated apply") {
    val config = ConfigFactory.parseString("local-dc = dc1")
    (LoadBalancingConfig(config): @nowarn("cat=deprecation")) shouldEqual LoadBalancingConfig(localDc = "dc1")
    (LoadBalancingConfig(config, LoadBalancingConfig.Default): @nowarn("cat=deprecation")) shouldEqual
      LoadBalancingConfig(localDc = "dc1")
  }
}

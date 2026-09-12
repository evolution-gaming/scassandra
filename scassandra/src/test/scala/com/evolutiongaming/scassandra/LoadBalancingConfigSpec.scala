package com.evolutiongaming.scassandra

import cats.implicits.*
import com.datastax.driver.core.policies.{DCAwareRoundRobinPolicy, TokenAwarePolicy}
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
    val expected = LoadBalancingConfig(
      localDc = "local",
      allowRemoteDcsForLocalConsistencyLevel = true,
    )
    ConfigSource.fromConfig(config).load[LoadBalancingConfig] shouldEqual expected.asRight
  }

  test("asJava wraps a DC aware policy into a token aware one") {
    val policy = LoadBalancingConfig(localDc = "dc1").asJava
    policy.map(_.getClass) shouldEqual Some(classOf[TokenAwarePolicy])
    policy.map(_.asInstanceOf[TokenAwarePolicy].getChildPolicy.getClass) shouldEqual
      Some(classOf[DCAwareRoundRobinPolicy])
  }

  test("asJava is None for an empty local DC") {
    LoadBalancingConfig(localDc = "").asJava shouldEqual None
  }

  test("fromConfig falls back to default on invalid config") {
    val config = ConfigFactory.parseString("allow-remote-dcs-for-local-consistency-level = nope")
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

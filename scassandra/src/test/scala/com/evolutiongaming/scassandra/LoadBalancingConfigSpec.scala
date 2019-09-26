package com.evolutiongaming.scassandra

import cats.implicits._
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}
import pureconfig.ConfigSource

class LoadBalancingConfigSpec extends FunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[LoadBalancingConfig] shouldEqual LoadBalancingConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("load-balancing.conf"))
    val expected = LoadBalancingConfig(
      localDc = "local",
      allowRemoteDcsForLocalConsistencyLevel = true)
    ConfigSource.fromConfig(config).load[LoadBalancingConfig] shouldEqual expected.asRight
  }
}
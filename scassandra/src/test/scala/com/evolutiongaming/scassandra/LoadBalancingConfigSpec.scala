package com.evolutiongaming.scassandra
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LoadBalancingConfigSpec extends AnyFunSuite with Matchers {

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
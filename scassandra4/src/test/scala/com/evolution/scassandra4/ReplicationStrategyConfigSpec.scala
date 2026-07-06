package com.evolution.scassandra4

import cats.data.NonEmptyList
import cats.implicits._
import com.evolution.scassandra4.ReplicationStrategyConfig.NetworkTopology.DcFactor
import com.evolution.scassandra4.ReplicationStrategyConfig._
import com.evolution.scassandra4.syntax._
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ReplicationStrategyConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    val expected = ReplicationStrategyConfig.Default
    ConfigSource.fromConfig(config).load[ReplicationStrategyConfig] shouldEqual expected.asRight
  }

  test("apply from simple config") {
    val config = ConfigFactory.parseURL(getClass.getResource("replication-strategy.conf"))
    val expected = Simple(2)
    ConfigSource.fromConfig(config.getConfig("simple")).load[ReplicationStrategyConfig] shouldEqual expected.asRight
  }

  test("apply from empty simple config") {
    val config = ConfigFactory.empty()
    val expected = Simple()
    ConfigSource.fromConfig(config).load[Simple] shouldEqual expected.asRight
  }

  test("apply from network topology config") {
    val config = ConfigFactory.parseURL(getClass.getResource("replication-strategy.conf"))
    val expected = NetworkTopology(NonEmptyList.of(DcFactor("dc1", 2), DcFactor("dc2", 3)))
    ConfigSource.fromConfig(config.getConfig("network-topology")).load[ReplicationStrategyConfig] shouldEqual expected.asRight
  }

  test("Simple.toCql") {
    Simple(2).toCql shouldEqual s"'SimpleStrategy','replication_factor':2"
  }

  test("NetworkTopology.toCql") {
    val config = NetworkTopology(NonEmptyList.of(DcFactor("dc1", 2), DcFactor("dc2", 3)))
    config.toCql shouldEqual "'NetworkTopologyStrategy','dc1':2,'dc2':3"
  }
}

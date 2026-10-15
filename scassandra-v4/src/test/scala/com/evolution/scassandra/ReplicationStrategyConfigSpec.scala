package com.evolution.scassandra

import cats.data.NonEmptyList
import cats.syntax.all.*
import com.evolution.scassandra.ReplicationStrategyConfig.*
import com.evolution.scassandra.ReplicationStrategyConfig.NetworkTopology.DcFactor
import com.evolution.scassandra.syntax.*
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class ReplicationStrategyConfigSpec extends AnyFunSuite with Matchers {

  private lazy val config = ConfigFactory.parseURL(getClass.getResource("replication-strategy.conf"))

  test("apply from empty config") {
    ConfigSource.fromConfig(ConfigFactory.empty()).load[ReplicationStrategyConfig] shouldEqual
      ReplicationStrategyConfig.Default.asRight
  }

  test("apply from simple config") {
    ConfigSource.fromConfig(config.getConfig("simple")).load[ReplicationStrategyConfig] shouldEqual
      Simple(2).asRight
  }

  test("apply from empty simple config") {
    ConfigSource.fromConfig(ConfigFactory.empty()).load[Simple] shouldEqual Simple().asRight
  }

  test("apply from network topology config") {
    val expected = NetworkTopology(NonEmptyList.of(DcFactor("dc1", 2), DcFactor("dc2", 3)))
    ConfigSource.fromConfig(config.getConfig("network-topology")).load[ReplicationStrategyConfig] shouldEqual
      expected.asRight
  }

  test("network topology without the block is the default") {
    val config = ConfigFactory.parseString("""replication-strategy = "NetworkTopology"""")
    ConfigSource.fromConfig(config).load[ReplicationStrategyConfig] shouldEqual
      NetworkTopology.Default.asRight
  }

  test("unknown strategy fails") {
    val config = ConfigFactory.parseString("""replication-strategy = "Local"""")
    ConfigSource.fromConfig(config).load[ReplicationStrategyConfig].isLeft shouldEqual true
  }

  test("invalid replication factor fails") {
    val config = ConfigFactory.parseString(
      """replication-strategy = "NetworkTopology", network-topology.replication-factors = ["dc1:a"]""",
    )
    ConfigSource.fromConfig(config).load[ReplicationStrategyConfig].isLeft shouldEqual true
  }

  test("Simple.toCql") {
    Simple(2).toCql shouldEqual s"'SimpleStrategy','replication_factor':2"
  }

  test("NetworkTopology.toCql") {
    val config = NetworkTopology(NonEmptyList.of(DcFactor("dc1", 2), DcFactor("dc2", 3)))
    config.toCql shouldEqual "'NetworkTopologyStrategy','dc1':2,'dc2':3"
  }

  test("CreateKeyspaceIfNotExists") {
    CreateKeyspaceIfNotExists("keyspace", Simple(2)) shouldEqual
      "CREATE KEYSPACE IF NOT EXISTS keyspace WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':2 }"
  }
}

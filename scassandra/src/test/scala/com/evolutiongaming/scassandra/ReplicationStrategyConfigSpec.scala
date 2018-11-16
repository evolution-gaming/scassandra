package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.ReplicationStrategyConfig.NetworkTopology.DcFactor
import com.evolutiongaming.scassandra.ReplicationStrategyConfig._
import com.evolutiongaming.scassandra.syntax._
import com.evolutiongaming.nel.Nel
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}

class ReplicationStrategyConfigSpec extends FunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ReplicationStrategyConfig(config) shouldEqual ReplicationStrategyConfig.Default
  }

  test("apply from simple config") {
    val config = ConfigFactory.parseURL(getClass.getResource("replication-strategy.conf"))
    val expected = Simple(2)
    ReplicationStrategyConfig(config.getConfig("simple")) shouldEqual expected
  }

  test("apply from network topology config") {
    val config = ConfigFactory.parseURL(getClass.getResource("replication-strategy.conf"))
    val expected = NetworkTopology(Nel(DcFactor("dc1", 2), DcFactor("dc2", 3)))
    ReplicationStrategyConfig(config.getConfig("network-topology")) shouldEqual expected
  }

  test("Simple.toCql") {
    Simple(2).toCql shouldEqual s"'SimpleStrategy','replication_factor':2"
  }

  test("NetworkTopology.toCql") {
    val config = NetworkTopology(Nel(DcFactor("dc1", 2), DcFactor("dc2", 3)))
    config.toCql shouldEqual "'NetworkTopologyStrategy','dc1':2,'dc2':3"
  }
}
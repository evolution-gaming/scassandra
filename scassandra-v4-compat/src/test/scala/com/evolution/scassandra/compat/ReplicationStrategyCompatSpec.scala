package com.evolution.scassandra.compat

import cats.data.NonEmptyList
import com.evolution.scassandra.ReplicationStrategyConfig
import com.evolution.scassandra.ReplicationStrategyConfig.NetworkTopology
import com.evolution.scassandra.ReplicationStrategyConfig.NetworkTopology.DcFactor
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ReplicationStrategyConfig as ReplicationStrategyConfigV3
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class ReplicationStrategyCompatSpec extends AnyFunSuite with Matchers {

  for (factors <- List("dc1:2,dc2:3", "dc1:2;dc2:3")) {
    test(s"replication-factors as a string $factors is read by the driver 3 and the driver 4 readers") {
      val config = ConfigFactory.parseString(
        s"""
           |replication-strategy = "NetworkTopology"
           |network-topology.replication-factors = "$factors"
           |""".stripMargin,
      )

      ConfigSource.fromConfig(config).load[ReplicationStrategyConfigV3] shouldEqual Right(
        ReplicationStrategyConfigV3.NetworkTopology(Nel(
          ReplicationStrategyConfigV3.NetworkTopology.DcFactor("dc1", 2),
          ReplicationStrategyConfigV3.NetworkTopology.DcFactor("dc2", 3),
        )),
      )

      ConfigSource.fromConfig(config).load[ReplicationStrategyConfig] shouldEqual Right(
        NetworkTopology(NonEmptyList.of(DcFactor("dc1", 2), DcFactor("dc2", 3))),
      )
    }
  }
}

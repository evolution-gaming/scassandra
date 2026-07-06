package com.evolution.scassandra4

import cats.data.NonEmptyList

/** Minimal configuration for the driver 4 based client.
  *
  * Phase 2 of MIGRATION_PLAN.md ports the full config tree from
  * `com.evolutiongaming.scassandra.CassandraConfig` (including the pureconfig
  * readers), keeping the same HOCON schema and translating it to a driver 4
  * `DriverConfigLoader`.
  *
  * @param name
  *   base of the session name; a unique cluster id is appended per created cluster
  * @param port
  *   default port for contact points given without an explicit `host:port`
  * @param contactPoints
  *   contact points in the form of `host` or `host:port`
  * @param localDatacenter
  *   local datacenter for the default load balancing policy; when empty, the
  *   datacenter is inferred from the contact points
  *   (`DcInferringLoadBalancingPolicy`) to keep driver 3 era configs working
  */
final case class CassandraConfig(
  name: String = "cluster",
  port: Int = 9042,
  contactPoints: NonEmptyList[String] = NonEmptyList.of("127.0.0.1"),
  localDatacenter: Option[String] = None,
)

object CassandraConfig {

  val Default: CassandraConfig = CassandraConfig()
}

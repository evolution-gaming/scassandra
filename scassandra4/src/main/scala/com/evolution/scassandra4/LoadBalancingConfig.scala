package com.evolution.scassandra4

import com.evolution.scassandra4.util.PureconfigSyntax._
import pureconfig.ConfigReader

/** Load balancing, keeping the driver 3 era schema.
  *
  * Driver 4's default load balancing policy is already token-aware and
  * datacenter-aware (driver 3 needed `TokenAwarePolicy(DCAwareRoundRobinPolicy)`
  * for the same behavior), so this config translates to the policy's options
  * rather than a policy class, see [[CreateDriverConfigLoader]]:
  *   - `localDc` → `basic.load-balancing-policy.local-datacenter`;
  *     when empty, the datacenter is inferred from the contact points
  *     (`DcInferringLoadBalancingPolicy`)
  *   - `allowRemoteDcsForLocalConsistencyLevel` →
  *     `advanced.load-balancing-policy.dc-failover.allow-for-local-consistency-levels`
  */
final case class LoadBalancingConfig(
  localDc: String = "localDc",
  allowRemoteDcsForLocalConsistencyLevel: Boolean = false)

object LoadBalancingConfig {

  val Default: LoadBalancingConfig = LoadBalancingConfig()

  implicit val configReaderLoadBalancingConfig: ConfigReader[LoadBalancingConfig] =
    ConfigReader.fromCursor[LoadBalancingConfig] { cursor =>
      val defaultConfig = LoadBalancingConfig()

      for {
        objCur <- cursor.asObjectCursor
        localDc <- objCur.getAtOpt[String]("local-dc").map(_.getOrElse(defaultConfig.localDc))
        allowRemoteDcsForLocalConsistencyLevel <- objCur.getAtOpt[Boolean]("allow-remote-dcs-for-local-consistency-level").map(_.getOrElse(defaultConfig.allowRemoteDcsForLocalConsistencyLevel))
      } yield LoadBalancingConfig(
        localDc = localDc,
        allowRemoteDcsForLocalConsistencyLevel = allowRemoteDcsForLocalConsistencyLevel
      )
    }
}

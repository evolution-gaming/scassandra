package com.evolution.scassandra.compat

import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.context.DriverContext
import com.datastax.oss.driver.api.core.metadata.Node
import com.datastax.oss.driver.internal.core.loadbalancing.DefaultLoadBalancingPolicy
import com.datastax.oss.driver.internal.core.loadbalancing.helper.OptionalLocalDcHelper
import org.slf4j.{Logger, LoggerFactory}

import java.util.{Map as JavaMap, Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/**
 * Load balancing policy of the configurations translated from scassandra for the Java
 * driver 3.
 *
 * The local DC is `basic.load-balancing-policy.local-datacenter` if set, otherwise the DC
 * of the first contact point, as chosen by `DCAwareRoundRobinPolicy` of the Java driver 3.
 * If the contact points are in different DCs, a warning is logged, while
 * `DcInferringLoadBalancingPolicy` of the Java driver 4 fails.
 */
class FirstContactPointDcLoadBalancingPolicy(driverContext: DriverContext, profileName: String)
extends DefaultLoadBalancingPolicy(driverContext, profileName) {

  import FirstContactPointDcLoadBalancingPolicy.*

  override protected def discoverLocalDc(nodes: JavaMap[UUID, Node]): Optional[String] = {
    new OptionalLocalDcHelper(context, profile, logPrefix)
      .discoverLocalDc(nodes)
      .toScala
      .orElse(firstContactPointDc)
      .toJava
  }

  private def firstContactPointDc: Option[String] = {
    val contactPoints = context.getMetadataManager.getContactPoints.asScala.toList
    val localDc = contactPoints.flatMap { node => Option(node.getDatacenter) }.headOption.getOrElse {
      throw new IllegalStateException(
        s"The local DC could not be inferred from the contact points, set ${ LocalDcOption } explicitly",
      )
    }
    val otherDcs = contactPoints.filterNot { node => Option(node.getDatacenter).contains(localDc) }
    if (otherDcs.isEmpty) {
      log.info(s"[$logPrefix] Inferred local DC from contact points: $localDc")
    } else {
      log.warn(
        s"[$logPrefix] No local DC was set, using the DC of the first contact point: $localDc, " +
          s"contact points in other DCs: ${ otherDcs.map(describe).mkString(", ") }; set ${ LocalDcOption } explicitly",
      )
    }
    Some(localDc)
  }

  private def describe(node: Node): String =
    s"${ node.getEndPoint } (${ Option(node.getDatacenter).getOrElse("unknown") })"

}

object FirstContactPointDcLoadBalancingPolicy {

  private val log: Logger = LoggerFactory.getLogger(classOf[FirstContactPointDcLoadBalancingPolicy])

  private val LocalDcOption: String = DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER.getPath
}

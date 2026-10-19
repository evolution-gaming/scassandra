package com.evolution.scassandra.compat

import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.context.DriverContext
import com.datastax.oss.driver.api.core.metadata.Node
import com.datastax.oss.driver.internal.core.loadbalancing.DefaultLoadBalancingPolicy
import com.datastax.oss.driver.internal.core.loadbalancing.helper.OptionalLocalDcHelper
import com.datastax.oss.driver.internal.core.util.AddressUtils
import org.slf4j.{Logger, LoggerFactory}

import java.net.SocketAddress
import java.util.{Collections, Map as JavaMap, Optional, UUID}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.Try

/**
 * Load balancing policy of the configurations translated from scassandra for the Java
 * driver 3.
 *
 * The local DC is `basic.load-balancing-policy.local-datacenter` if set, otherwise the DC
 * of the first contact point in `basic.contact-points`, as `DCAwareRoundRobinPolicy` of
 * the Java driver 3 took the DC of a contact point. If the contact points are in
 * different DCs, a warning is logged, while `DcInferringLoadBalancingPolicy` of the Java
 * driver 4 fails.
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
    val contactPoints = context.getMetadataManager.getContactPoints.asScala.toList.map { node =>
      ContactPoint(node.getEndPoint.resolve(), Option(node.getDatacenter))
    }
    localDcOf(contactPoints, configuredContactPoints) match {
      case None =>
        throw new IllegalStateException(
          s"The local DC could not be inferred from the contact points, set $LocalDcOption explicitly",
        )
      case Some(LocalDc(localDc, Nil)) =>
        log.info(s"[$logPrefix] Inferred local DC from contact points: $localDc")
        Some(localDc)
      case Some(LocalDc(localDc, otherDcs)) =>
        log.warn(
          s"[$logPrefix] No local DC was set, using the DC of the first contact point: $localDc, " +
            s"contact points in other DCs: ${ otherDcs.mkString(", ") }; set $LocalDcOption explicitly",
        )
        Some(localDc)
    }
  }

  // the driver keeps the contact points in a hash set, so their order is taken from the configuration
  private def configuredContactPoints: List[SocketAddress] = {
    val config = context.getConfig.getDefaultProfile
    val resolve = config.getBoolean(DefaultDriverOption.RESOLVE_CONTACT_POINTS, true)
    config
      .getStringList(DefaultDriverOption.CONTACT_POINTS, Collections.emptyList[String]())
      .asScala
      .toList
      .flatMap { contactPoint =>
        Try(AddressUtils.extract(contactPoint, resolve).asScala.toList).getOrElse(Nil)
      }
  }
}

object FirstContactPointDcLoadBalancingPolicy {

  private val log: Logger = LoggerFactory.getLogger(classOf[FirstContactPointDcLoadBalancingPolicy])

  private val LocalDcOption: String = DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER.getPath

  private[compat] final case class ContactPoint(address: SocketAddress, datacenter: Option[String]) {
    override def toString: String = s"$address (${ datacenter.getOrElse("unknown DC") })"
  }

  /**
   * @param otherDcs
   *   contact points known to be in other DCs
   */
  private[compat] final case class LocalDc(name: String, otherDcs: List[ContactPoint])

  /**
   * Takes the DC of the first contact point with a known DC, in the order of
   * `configured`, the contact points not found in `configured` go last
   */
  private[compat] def localDcOf(contactPoints: List[ContactPoint], configured: List[SocketAddress])
    : Option[LocalDc] = {
    val ordered = contactPoints.sortBy { contactPoint =>
      configured.indexOf(contactPoint.address) match {
        case -1 => Int.MaxValue
        case index => index
      }
    }
    ordered.flatMap(_.datacenter).headOption.map { localDc =>
      LocalDc(localDc, ordered.filter(_.datacenter.exists(_ != localDc)))
    }
  }
}

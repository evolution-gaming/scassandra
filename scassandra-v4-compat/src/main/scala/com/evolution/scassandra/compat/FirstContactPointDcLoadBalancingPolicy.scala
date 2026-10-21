package com.evolution.scassandra.compat

import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.context.DriverContext
import com.datastax.oss.driver.api.core.metadata.Node
import com.datastax.oss.driver.internal.core.loadbalancing.DefaultLoadBalancingPolicy
import com.datastax.oss.driver.internal.core.loadbalancing.helper.OptionalLocalDcHelper
import com.datastax.oss.driver.internal.core.util.AddressUtils
import org.slf4j.{Logger, LoggerFactory}

import java.net.InetSocketAddress
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
 * the Java driver 3 took the DC of a contact point. A warning is logged if the contact
 * points are in different DCs, or if the DC is taken from another contact point than the
 * first one, while `DcInferringLoadBalancingPolicy` of the Java driver 4 fails.
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
      ContactPoint(hostPortOf(node), Option(node.getDatacenter))
    }
    localDcOf(contactPoints, configuredContactPoints) match {
      case None =>
        throw new IllegalStateException(
          s"The local DC could not be inferred from the contact points, set $LocalDcOption explicitly",
        )
      case Some(localDc) =>
        warningOf(localDc).fold {
          log.info(s"[$logPrefix] Inferred local DC from contact points: ${ localDc.name }")
        } { warning =>
          log.warn(s"[$logPrefix] $warning")
        }
        Some(localDc.name)
    }
  }

  // the driver keeps the contact points in a hash set, so their order is taken from the
  // configuration, matching the host names as configured, without resolving them again
  private def configuredContactPoints: List[HostPort] = {
    context.getConfig.getDefaultProfile
      .getStringList(DefaultDriverOption.CONTACT_POINTS, Collections.emptyList[String]())
      .asScala
      .toList
      .flatMap { contactPoint =>
        Try(AddressUtils.extract(contactPoint, false).asScala.toList).getOrElse(Nil).map { address =>
          HostPort(address.getHostString, address.getPort)
        }
      }
  }
}

object FirstContactPointDcLoadBalancingPolicy {

  private val log: Logger = LoggerFactory.getLogger(classOf[FirstContactPointDcLoadBalancingPolicy])

  private val LocalDcOption: String = DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER.getPath

  private val ContactPointsOption: String = DefaultDriverOption.CONTACT_POINTS.getPath

  private[compat] final case class HostPort(host: String, port: Int) {
    override def toString: String = s"$host:$port"
  }

  private[compat] final case class ContactPoint(hostPort: HostPort, datacenter: Option[String]) {
    override def toString: String = s"$hostPort (${ datacenter.getOrElse("unknown DC") })"
  }

  /**
   * @param contactPoint
   *   the contact point the DC is taken from
   * @param configured
   *   whether `contactPoint` is found in `basic.contact-points`
   * @param skipped
   *   the contact points configured before `contactPoint`, skipped as their DC is unknown
   * @param otherDcs
   *   the contact points known to be in other DCs
   */
  private[compat] final case class LocalDc(
    name: String,
    contactPoint: ContactPoint,
    configured: Boolean,
    skipped: List[ContactPoint],
    otherDcs: List[ContactPoint],
  )

  /**
   * Takes the DC of the first contact point with a known DC, in the order of
   * `configured`, the contact points not found in `configured` go last
   */
  private[compat] def localDcOf(contactPoints: List[ContactPoint], configured: List[HostPort])
    : Option[LocalDc] = {
    def indexOf(contactPoint: ContactPoint): Int = configured.indexOf(contactPoint.hostPort) match {
      case -1 => Int.MaxValue
      case index => index
    }
    val ordered = contactPoints.sortBy(indexOf)
    for {
      contactPoint <- ordered.find(_.datacenter.isDefined)
      name <- contactPoint.datacenter
    } yield LocalDc(
      name = name,
      contactPoint = contactPoint,
      configured = configured.contains(contactPoint.hostPort),
      // a host name might resolve to several addresses, the ones not in the cluster are not reported
      skipped = ordered.filter { other =>
        other.datacenter.isEmpty && indexOf(other) < indexOf(contactPoint)
      },
      otherDcs = ordered.filter(_.datacenter.exists(_ != name)),
    )
  }

  /**
   * Explains why the local DC might be wrong, if it might
   */
  private[compat] def warningOf(localDc: LocalDc): Option[String] = {
    val reasons = List(
      Option.when(!localDc.configured) {
        s"it is not found in $ContactPointsOption, as resolved by the driver"
      },
      Option.when(localDc.skipped.nonEmpty) {
        s"the DC of the contact points before it is unknown: ${ localDc.skipped.mkString(", ") }"
      },
      Option.when(localDc.otherDcs.nonEmpty) {
        s"contact points in other DCs: ${ localDc.otherDcs.mkString(", ") }"
      },
    ).flatten
    Option.when(reasons.nonEmpty) {
      s"No local DC was set, using ${ localDc.name }, the DC of the contact point ${ localDc.contactPoint.hostPort }, " +
        s"while ${ reasons.mkString("; ") }; set $LocalDcOption explicitly"
    }
  }

  private def hostPortOf(node: Node): HostPort = {
    node.getEndPoint.resolve() match {
      case address: InetSocketAddress => HostPort(address.getHostString, address.getPort)
      case address => HostPort(address.toString, -1)
    }
  }
}

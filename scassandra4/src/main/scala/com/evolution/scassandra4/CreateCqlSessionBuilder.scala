package com.evolution.scassandra4

import com.datastax.oss.driver.api.core.{CqlSession, CqlSessionBuilder}
import com.datastax.oss.driver.api.core.config.{DefaultDriverOption, DriverConfigLoader}

import java.net.InetSocketAddress
import scala.jdk.CollectionConverters._

object CreateCqlSessionBuilder {

  def apply(config: CassandraConfig, clusterId: Int): CqlSessionBuilder = {

    val contactPoints = config.contactPoints.map { contactPoint =>
      contactPoint.split(":").map(_.trim) match {
        case Array(host, port) => new InetSocketAddress(host, port.toInt)
        case Array(host)       => new InetSocketAddress(host, config.port)
        case _                 =>
          val msg = s"A contact point should be in form of [host:port] or [host], but is $contactPoint"
          throw new IllegalArgumentException(msg)
      }
    }

    val sessionName = s"${ config.name }-$clusterId"

    val configLoader = {
      val builder = DriverConfigLoader
        .programmaticBuilder()
        .withString(DefaultDriverOption.SESSION_NAME, sessionName)
      config.localDatacenter
        .fold {
          // the default load balancing policy of driver 4 refuses to start without
          // an explicit local datacenter, infer it from the contact points instead
          builder.withString(
            DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS,
            "DcInferringLoadBalancingPolicy",
          )
        } { _ => builder }
        .build()
    }

    val builder = CqlSession
      .builder()
      .addContactPoints(contactPoints.toList.asJava)
      .withConfigLoader(configLoader)

    config.localDatacenter.fold(builder)(builder.withLocalDatacenter)
  }
}

package com.evolution.scassandra4

import com.datastax.oss.driver.api.core.{CqlSession, CqlSessionBuilder}

import java.net.{InetSocketAddress, URL}
import java.nio.file.Paths
import scala.jdk.CollectionConverters._

object CreateCqlSessionBuilder {

  def apply(config: CassandraConfig, clusterId: Int): CqlSessionBuilder = {

    val sessionName = s"${ config.name }-$clusterId"

    val builder = CqlSession
      .builder()
      .withConfigLoader(CreateDriverConfigLoader(config, sessionName))

    config.cloudSecureConnectBundle match {
      case Some(CloudSecureConnectBundleConfig.File(path)) =>
        builder.withCloudSecureConnectBundle(Paths.get(path))
      case Some(CloudSecureConnectBundleConfig.Url(url))   =>
        builder.withCloudSecureConnectBundle(new URL(url))
      case None                                            =>
        val contactPoints = config.contactPoints.map { contactPoint =>
          contactPoint.split(":").map(_.trim) match {
            case Array(host, port) => new InetSocketAddress(host, port.toInt)
            case Array(host)       => new InetSocketAddress(host, config.port)
            case _                 =>
              val msg = s"A contact point should be in form of [host:port] or [host], but is $contactPoint"
              throw new IllegalArgumentException(msg)
          }
        }
        builder.addContactPoints(contactPoints.toList.asJava)
    }
  }
}

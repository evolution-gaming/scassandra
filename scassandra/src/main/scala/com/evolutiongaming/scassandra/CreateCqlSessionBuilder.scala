package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.{CqlSession, CqlSessionBuilder}

import java.net.{InetSocketAddress, URI}
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*

object CreateCqlSessionBuilder {

  def apply(config: CassandraConfig, sessionName: String): CqlSessionBuilder = {
    val builder = CqlSession
      .builder()
      .withConfigLoader(CreateDriverConfigLoader(config, sessionName))

    config.cloudSecureConnectBundle match {
      case Some(CloudSecureConnectBundleConfig.File(path)) =>
        builder.withCloudSecureConnectBundle(Paths.get(path))
      case Some(CloudSecureConnectBundleConfig.Url(url)) =>
        builder.withCloudSecureConnectBundle(URI.create(url).toURL)
      case None => builder.addContactPoints(contactPoints(config).asJava)
    }
  }

  def contactPoints(config: CassandraConfig): List[InetSocketAddress] = {
    config.contactPoints.toList.map { contactPoint =>
      contactPoint.split(":").map(_.trim) match {
        case Array(host, port) => new InetSocketAddress(host, port.toInt)
        case Array(host) => new InetSocketAddress(host, config.port)
        case _ =>
          val msg = s"A contact point should be in form of [host:port] or [host], but is $contactPoint"
          throw new IllegalArgumentException(msg)
      }
    }
  }
}

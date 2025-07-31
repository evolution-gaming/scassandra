package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.session.{QueryLogger, Cluster as ClusterJ}
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.util.ToJava

import java.net.{InetSocketAddress, URI, URL}
import java.nio.file.Path

object CreateClusterJ {
  def apply(config: CassandraConfig, clusterId: Int): CqlSession = {
    val port = config.port

    val contactPoints: Nel[InetSocketAddress] = config.contactPoints.map { contactPoint =>
      contactPoint.split(":").map(_.trim) match {
        case Array(host, port) => new InetSocketAddress(host, port.toInt)
        case Array(host)       => new InetSocketAddress(host, port)
        case _                 =>
          val msg = s"A contact point should be in form of [host:port] or [host], but is $contactPoint"
          throw new IllegalArgumentException(msg)
      }
    }

    val builder = CqlSession.builder()

    config.cloudSecureConnectBundle match {
      case Some(CloudSecureConnectBundleConfig.File(path)) =>
        builder.withCloudSecureConnectBundle(Path.of(new URI(path)))
      case Some(CloudSecureConnectBundleConfig.Url(url))   =>
        builder.withCloudSecureConnectBundle(new URI(url).toURL)
      case None                                            =>
        builder.addContactPoints(ToJava.from(contactPoints.toList))
    }

    builder
//      .withClusterName(clusterName)
      .withPoolingOptions(config.pooling.asJava)
      .withReconnectionPolicy(config.reconnection.asJava)
      .withQueryOptions(config.query.asJava)
      .withSocketOptions(config.socket.asJava)
      .withCompression(config.compression)
      .withPort(port)

    config.protocolVersion foreach { x => builder.withProtocolVersion(x) }
    config.authentication.foreach { x => builder.withCredentials(x.username, x.password.value) }
    config.loadBalancing.foreach { x => x.asJava.foreach { builder.withLoadBalancingPolicy } }
    config.speculativeExecution.foreach { x => builder.withSpeculativeExecutionPolicy(x.asJava) }
    if (!config.jmxReporting) builder.withoutJMXReporting()
    if (!config.metrics) builder.withoutMetrics()

    val cluster = builder.build()

    if (config.logQueries) {
      val logger = QueryLogger.builder().build()
      cluster.register(logger)
    }

    cluster
  }
}

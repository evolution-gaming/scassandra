package com.evolutiongaming.scassandra

import java.net.InetSocketAddress

import com.datastax.driver.core.{QueryLogger, Cluster => ClusterJ}
import com.evolutiongaming.util.ToJava

object CreateClusterJ {

  def apply(config: CassandraConfig, clusterId: Int): ClusterJ = {

    val port = config.port

    val contactPoints = config.contactPoints.map { contactPoint =>
      contactPoint.split(":").map(_.trim) match {
        case Array(host, port) => new InetSocketAddress(host, port.toInt)
        case Array(host)       => new InetSocketAddress(host, port)
        case _                 =>
          val msg = s"A contact point should be in form of [host:port] or [host], but is $contactPoint"
          throw new IllegalArgumentException(msg)
      }
    }

    val clusterName = s"${ config.name }-$clusterId"

    val builder = ClusterJ.builder
      .addContactPointsWithPorts(ToJava.from(contactPoints.toList))
      .withClusterName(clusterName)
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

    val cluster = builder.build()

    if (config.logQueries) {
      val logger = QueryLogger.builder().build()
      cluster.register(logger)
    }

    cluster
  }
}

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

  private val Ipv6 = """\[([^\]]+)\](?::(.+))?""".r
  private val HostPort = """([^:]+)(?::(.+))?""".r

  def contactPoints(config: CassandraConfig): List[InetSocketAddress] = {
    config.contactPoints.toList.map { contactPoint =>
      def invalid = {
        val msg =
          s"A contact point should be in form of host, host:port, [ipv6] or [ipv6]:port, but is $contactPoint"
        throw new IllegalArgumentException(msg)
      }
      def address(host: String, port: String) = {
        val value = Option(port).fold(Option(config.port))(_.trim.toIntOption).getOrElse(invalid)
        new InetSocketAddress(host.trim, value)
      }
      contactPoint.trim match {
        case Ipv6(host, port) => address(host, port)
        case HostPort(host, port) => address(host, port)
        case _ => invalid
      }
    }
  }
}

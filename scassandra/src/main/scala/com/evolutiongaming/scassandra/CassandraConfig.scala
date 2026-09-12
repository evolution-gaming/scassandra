package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.{DefaultProtocolVersion, ProtocolVersion}
import com.evolutiongaming.config.ConfigHelper.*
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ConfigHelpers.*
import com.evolutiongaming.scassandra.util.ConfigReaderFromEnum
import com.typesafe.config.Config
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

/**
 * Translated into the driver configuration by [[CreateDriverConfigLoader]], see
 * [[https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/configuration/]]
 *
 * If a cloud secure connect bundle is specified, the contact points and port settings
 * will be ignored.
 */
final case class CassandraConfig(
  name: String = "cluster",
  port: Int = 9042,
  contactPoints: Nel[String] = Nel("127.0.0.1"),
  protocolVersion: Option[ProtocolVersion] = None,
  pooling: PoolingConfig = PoolingConfig.Default,
  query: QueryConfig = QueryConfig.Default,
  reconnection: ReconnectionConfig = ReconnectionConfig.Default,
  socket: SocketConfig = SocketConfig.Default,
  authentication: Option[AuthenticationConfig] = None,
  loadBalancing: Option[LoadBalancingConfig] = None,
  speculativeExecution: Option[SpeculativeExecutionConfig] = None,
  compression: Compression = Compression.None,
  logQueries: Boolean = false,
  cloudSecureConnectBundle: Option[CloudSecureConnectBundleConfig] = None,
)

object CassandraConfig {

  val Default: CassandraConfig = CassandraConfig()

  implicit val configReaderProtocolVersion: ConfigReader[ProtocolVersion] = {
    ConfigReaderFromEnum(DefaultProtocolVersion.values()).map[ProtocolVersion](identity)
  }

  implicit val configReaderCassandraConfig: ConfigReader[CassandraConfig] = (cursor: ConfigCursor) => {
    for {
      cursor <- cursor.asObjectCursor
    } yield {
      fromConfig(cursor.objValue.toConfig, Default)
    }
  }

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): CassandraConfig = fromConfig(config, Default)

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config, default: => CassandraConfig): CassandraConfig = fromConfig(config, default)

  def fromConfig(config: Config, default: => CassandraConfig): CassandraConfig = {

    val source = ConfigSource.fromConfig(config)

    def get[A: ConfigReader](name: String) = source.at(name).load[A]

    val pooling = get[PoolingConfig]("pooling") getOrElse default.pooling
    val query = get[QueryConfig]("query") getOrElse default.query
    val reconnection = get[ReconnectionConfig]("reconnection") getOrElse default.reconnection
    val socket = get[SocketConfig]("socket") getOrElse default.socket
    val authentication = get[AuthenticationConfig]("authentication").toOption orElse default.authentication
    val loadBalancing = get[LoadBalancingConfig]("load-balancing").toOption orElse default.loadBalancing
    val speculativeExecution = get[SpeculativeExecutionConfig]("speculative-execution").toOption
      .orElse(default.speculativeExecution)
    val cloudSecureConnectBundle = get[CloudSecureConnectBundleConfig](
      "cloud-secure-connect-bundle",
    ).toOption orElse default.cloudSecureConnectBundle

    CassandraConfig(
      name = get[String]("name") getOrElse default.name,
      port = get[Int]("port") getOrElse default.port,
      contactPoints = config.getOpt[Nel[String]]("contact-points") getOrElse default.contactPoints,
      cloudSecureConnectBundle = cloudSecureConnectBundle,
      protocolVersion = get[ProtocolVersion]("protocol-version").toOption orElse default.protocolVersion,
      pooling = pooling,
      query = query,
      reconnection = reconnection,
      socket = socket,
      authentication = authentication,
      loadBalancing = loadBalancing,
      speculativeExecution = speculativeExecution,
      compression = get[Compression]("compression") getOrElse default.compression,
      logQueries = get[Boolean]("log-queries") getOrElse default.logQueries,
    )
  }
}

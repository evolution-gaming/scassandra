package com.evolution.scassandra4

import cats.data.NonEmptyList
import com.datastax.oss.driver.api.core.DefaultProtocolVersion
import com.evolution.scassandra4.util.ConfigReaderFromEnum
import com.typesafe.config.Config
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

/** Configuration for the driver 4 based client, keeping the HOCON schema of
  * `com.evolutiongaming.scassandra.CassandraConfig` so that existing
  * deployment configs keep working after a migration.
  *
  * The config is translated to driver 4's own configuration, see
  * [[CreateDriverConfigLoader]] for the mapping and for the fields that have
  * no driver 4 counterpart (`jmxReporting` and `metrics` among them — driver 4
  * metrics require a registry and are not translated yet).
  *
  * If a cloud secure connect bundle is specified, the contact points and port
  * settings will be ignored.
  */
final case class CassandraConfig(
  name: String = "cluster",
  port: Int = 9042,
  contactPoints: NonEmptyList[String] = NonEmptyList.of("127.0.0.1"),
  protocolVersion: Option[DefaultProtocolVersion] = None,
  pooling: PoolingConfig = PoolingConfig.Default,
  query: QueryConfig = QueryConfig.Default,
  reconnection: ReconnectionConfig = ReconnectionConfig.Default,
  socket: SocketConfig = SocketConfig.Default,
  authentication: Option[AuthenticationConfig] = None,
  loadBalancing: Option[LoadBalancingConfig] = None,
  speculativeExecution: Option[SpeculativeExecutionConfig] = None,
  compression: Compression = Compression.None,
  logQueries: Boolean = false,
  jmxReporting: Boolean = false,
  cloudSecureConnectBundle: Option[CloudSecureConnectBundleConfig] = None,
  metrics: Boolean = false
)

object CassandraConfig {

  val Default: CassandraConfig = CassandraConfig()

  implicit val configReaderProtocolVersion: ConfigReader[DefaultProtocolVersion] =
    ConfigReaderFromEnum(DefaultProtocolVersion.values())

  implicit val configReaderCassandraConfig: ConfigReader[CassandraConfig] = {
    (cursor: ConfigCursor) => {
      for {
        cursor <- cursor.asObjectCursor
      } yield {
        fromConfig(cursor.objValue.toConfig, Default)
      }
    }
  }


  def fromConfig(config: Config, default: => CassandraConfig): CassandraConfig = {

    val source = ConfigSource.fromConfig(config)

    def get[A: ConfigReader](name: String) = source.at(name).load[A]

    val contactPoints = {
      val fromList = get[List[String]]("contact-points")
      val fromString = get[String]("contact-points").map { _.split(",").toList.map(_.trim).filter(_.nonEmpty) }
      (fromList orElse fromString)
        .toOption
        .flatMap { NonEmptyList.fromList }
        .getOrElse(default.contactPoints)
    }

    val pooling = get[PoolingConfig]("pooling") getOrElse default.pooling
    val query = get[QueryConfig]("query") getOrElse default.query
    val reconnection = get[ReconnectionConfig]("reconnection") getOrElse default.reconnection
    val socket = get[SocketConfig]("socket") getOrElse default.socket
    val authentication = get[AuthenticationConfig]("authentication").toOption orElse default.authentication
    val loadBalancing = get[LoadBalancingConfig]("load-balancing").toOption orElse default.loadBalancing
    val speculativeExecution = get[SpeculativeExecutionConfig]("speculative-execution").toOption orElse default.speculativeExecution
    val cloudSecureConnectBundle = get[CloudSecureConnectBundleConfig](
      "cloud-secure-connect-bundle",
    ).toOption orElse default.cloudSecureConnectBundle

    CassandraConfig(
      name = get[String]("name") getOrElse default.name,
      port = get[Int]("port") getOrElse default.port,
      contactPoints = contactPoints,
      cloudSecureConnectBundle = cloudSecureConnectBundle,
      protocolVersion = get[DefaultProtocolVersion]("protocol-version").toOption orElse default.protocolVersion,
      pooling = pooling,
      query = query,
      reconnection = reconnection,
      socket = socket,
      authentication = authentication,
      loadBalancing = loadBalancing,
      speculativeExecution = speculativeExecution,
      compression = get[Compression]("compression") getOrElse default.compression,
      logQueries = get[Boolean]("log-queries") getOrElse default.logQueries,
      jmxReporting = get[Boolean]("jmx-reporting") getOrElse default.jmxReporting,
      metrics = get[Boolean]("metrics") getOrElse default.metrics)
  }
}

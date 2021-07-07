package com.evolutiongaming.scassandra

import com.datastax.driver.core.ProtocolOptions.Compression
import com.datastax.driver.core.ProtocolVersion
import com.evolutiongaming.config.ConfigHelper._
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ConfigHelpers._
import com.evolutiongaming.scassandra.util.ConfigReaderFromEnum
import com.typesafe.config.Config
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/#setting-up-the-driver]]
  *
  * If a cloud secure connect bundle is specified, the contact points and port settings will be ignored.
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
  compression: Compression = Compression.NONE,
  logQueries: Boolean = false,
  jmxReporting: Boolean = false,
  cloudSecureConnectBundle: Option[CloudSecureConnectBundleConfig] = None) {

  //for binary compatibility
  private[scassandra] def this(
    name: String,
    port: Int,
    contactPoints: Nel[String],
    protocolVersion: Option[ProtocolVersion],
    pooling: PoolingConfig,
    query: QueryConfig,
    reconnection: ReconnectionConfig,
    socket: SocketConfig,
    authentication: Option[AuthenticationConfig],
    loadBalancing: Option[LoadBalancingConfig],
    speculativeExecution: Option[SpeculativeExecutionConfig],
    compression: Compression,
    logQueries: Boolean,
    jmxReporting: Boolean,
  ) = {
    this(
      name = name,
      port = port,
      contactPoints = contactPoints,
      protocolVersion = protocolVersion,
      pooling = pooling,
      query = query,
      reconnection = reconnection,
      socket = socket,
      authentication = authentication,
      loadBalancing = loadBalancing,
      speculativeExecution = speculativeExecution,
      compression = compression,
      logQueries = logQueries,
      jmxReporting = jmxReporting,
      cloudSecureConnectBundle = None,
    )
  }

  //for binary compatibility
  def copy(
    name: String = this.name,
    port: Int = this.port,
    contactPoints: Nel[String] = this.contactPoints,
    protocolVersion: Option[ProtocolVersion] = this.protocolVersion,
    pooling: PoolingConfig = this.pooling,
    query: QueryConfig = this.query,
    reconnection: ReconnectionConfig = this.reconnection,
    socket: SocketConfig = this.socket,
    authentication: Option[AuthenticationConfig] = this.authentication,
    loadBalancing: Option[LoadBalancingConfig] = this.loadBalancing,
    speculativeExecution: Option[SpeculativeExecutionConfig] = this.speculativeExecution,
    compression: Compression = this.compression,
    logQueries: Boolean = this.logQueries,
    jmxReporting: Boolean = this.jmxReporting,
    cloudSecureConnectBundle: Option[CloudSecureConnectBundleConfig] = this.cloudSecureConnectBundle,
  ): CassandraConfig = new CassandraConfig(
    name = name,
    port = port,
    contactPoints = contactPoints,
    protocolVersion = protocolVersion,
    pooling = pooling,
    query = query,
    reconnection = reconnection,
    socket = socket,
    authentication = authentication,
    loadBalancing = loadBalancing,
    speculativeExecution = speculativeExecution,
    compression = compression,
    logQueries = logQueries,
    jmxReporting = jmxReporting,
    cloudSecureConnectBundle = cloudSecureConnectBundle,
  )

  //for binary compatibility
  private[scassandra] def copy(
    name: String,
    port: Int,
    contactPoints: Nel[String],
    protocolVersion: Option[ProtocolVersion],
    pooling: PoolingConfig,
    query: QueryConfig,
    reconnection: ReconnectionConfig,
    socket: SocketConfig,
    authentication: Option[AuthenticationConfig],
    loadBalancing: Option[LoadBalancingConfig],
    speculativeExecution: Option[SpeculativeExecutionConfig],
    compression: Compression,
    logQueries: Boolean,
    jmxReporting: Boolean,
  ): CassandraConfig = new CassandraConfig(
    name = name,
    port = port,
    contactPoints = contactPoints,
    protocolVersion = protocolVersion,
    pooling = pooling,
    query = query,
    reconnection = reconnection,
    socket = socket,
    authentication = authentication,
    loadBalancing = loadBalancing,
    speculativeExecution = speculativeExecution,
    compression = compression,
    logQueries = logQueries,
    jmxReporting = jmxReporting,
    cloudSecureConnectBundle = this.cloudSecureConnectBundle,
  )
}


object CassandraConfig {

  val Default: CassandraConfig = CassandraConfig()

  implicit val configReaderCompression: ConfigReader[Compression] = ConfigReaderFromEnum(Compression.values())

  implicit val configReaderProtocolVersion: ConfigReader[ProtocolVersion] = ConfigReaderFromEnum(ProtocolVersion.values())

  implicit val configReaderCassandraConfig: ConfigReader[CassandraConfig] = {
    cursor: ConfigCursor => {
      for {
        cursor <- cursor.asObjectCursor
      } yield {
        fromConfig(cursor.objValue.toConfig, Default)
      }
    }
  }


  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): CassandraConfig = fromConfig(config, Default)

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config, default: => CassandraConfig): CassandraConfig = fromConfig(config, default)

  //for binary compatibility
  private[scassandra] def apply(
    name: String,
    port: Int,
    contactPoints: Nel[String],
    protocolVersion: Option[ProtocolVersion],
    pooling: PoolingConfig,
    query: QueryConfig,
    reconnection: ReconnectionConfig,
    socket: SocketConfig,
    authentication: Option[AuthenticationConfig],
    loadBalancing: Option[LoadBalancingConfig],
    speculativeExecution: Option[SpeculativeExecutionConfig],
    compression: Compression,
    logQueries: Boolean,
    jmxReporting: Boolean,
  ): CassandraConfig = CassandraConfig(
    name = name,
    port = port,
    contactPoints = contactPoints,
    cloudSecureConnectBundle = None,
    protocolVersion = protocolVersion,
    pooling = pooling,
    query = query,
    reconnection = reconnection,
    socket = socket,
    authentication = authentication,
    loadBalancing = loadBalancing,
    speculativeExecution = speculativeExecution,
    compression = compression,
    logQueries = logQueries,
    jmxReporting = jmxReporting,
  )


  def fromConfig(config: Config, default: => CassandraConfig): CassandraConfig = {

    val source = ConfigSource.fromConfig(config)

    def get[A: ConfigReader](name: String) = source.at(name).load[A]

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
      jmxReporting = get[Boolean]("jmx-reporting") getOrElse default.jmxReporting)
  }
}

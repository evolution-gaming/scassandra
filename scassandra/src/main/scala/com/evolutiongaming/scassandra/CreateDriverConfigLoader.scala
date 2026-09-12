package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.config.DefaultDriverOption.*
import com.datastax.oss.driver.api.core.config.{DriverConfigLoader, ProgrammaticDriverConfigLoaderBuilder}

import java.time.Duration as DurationJ
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

/**
 * Translates [[CassandraConfig]] into the driver configuration, options not covered by
 * [[CassandraConfig]] keep the driver defaults.
 */
object CreateDriverConfigLoader {

  private type Builder = ProgrammaticDriverConfigLoaderBuilder

  def apply(config: CassandraConfig, sessionName: String): DriverConfigLoader = {
    val steps = List(
      base(config, sessionName),
      socket(config.socket),
      opt(config.protocolVersion)((builder, version) => builder.withString(PROTOCOL_VERSION, version.name)),
      when(config.compression != Compression.None) {
        _.withString(PROTOCOL_COMPRESSION, config.compression.name)
      },
      opt(config.authentication)(authentication),
      loadBalancing(config.loadBalancing),
      opt(config.speculativeExecution)(speculativeExecution),
      when(config.logQueries)(logQueries),
    )
    steps.foldLeft(DriverConfigLoader.programmaticBuilder()) { (builder, step) => step(builder) }.build()
  }

  private def base(config: CassandraConfig, sessionName: String): Builder => Builder = { builder =>
    val query = config.query
    val pooling = config.pooling
    val reconnection = config.reconnection
    builder
      .withString(SESSION_NAME, sessionName)
      .withString(REQUEST_CONSISTENCY, query.consistency.name)
      .withString(REQUEST_SERIAL_CONSISTENCY, query.serialConsistency.name)
      .withInt(REQUEST_PAGE_SIZE, query.fetchSize)
      .withBoolean(REQUEST_DEFAULT_IDEMPOTENCE, query.defaultIdempotence)
      .withBoolean(METADATA_SCHEMA_ENABLED, query.metadata)
      .withBoolean(METADATA_TOKEN_MAP_ENABLED, query.metadata)
      .withDuration(METADATA_SCHEMA_WINDOW, duration(query.refreshSchemaInterval))
      .withInt(METADATA_SCHEMA_MAX_EVENTS, query.maxPendingRefreshSchemaRequests)
      .withDuration(METADATA_TOPOLOGY_WINDOW, duration(query.refreshNodeListInterval))
      .withInt(METADATA_TOPOLOGY_MAX_EVENTS, query.maxPendingRefreshNodeListRequests)
      .withBoolean(REPREPARE_ENABLED, query.rePrepareOnUp)
      .withBoolean(PREPARE_ON_ALL_NODES, query.prepareOnAllHosts)
      .withString(RECONNECTION_POLICY_CLASS, "ExponentialReconnectionPolicy")
      .withDuration(RECONNECTION_BASE_DELAY, duration(reconnection.minDelay))
      .withDuration(RECONNECTION_MAX_DELAY, duration(reconnection.maxDelay))
      .withInt(CONNECTION_POOL_LOCAL_SIZE, pooling.localSize)
      .withInt(CONNECTION_POOL_REMOTE_SIZE, pooling.remoteSize)
      .withInt(CONNECTION_MAX_REQUESTS, pooling.maxRequestsPerConnection)
      .withDuration(HEARTBEAT_INTERVAL, duration(pooling.heartbeatInterval))
  }

  private def socket(config: SocketConfig): Builder => Builder = { builder =>
    val steps = List(
      opt(config.keepAlive)(_.withBoolean(SOCKET_KEEP_ALIVE, _)),
      opt(config.reuseAddress)(_.withBoolean(SOCKET_REUSE_ADDRESS, _)),
      opt(config.soLinger)(_.withInt(SOCKET_LINGER_INTERVAL, _)),
      opt(config.tcpNoDelay)(_.withBoolean(SOCKET_TCP_NODELAY, _)),
      opt(config.receiveBufferSize)(_.withInt(SOCKET_RECEIVE_BUFFER_SIZE, _)),
      opt(config.sendBufferSize)(_.withInt(SOCKET_SEND_BUFFER_SIZE, _)),
    )
    val base = builder
      .withDuration(CONNECTION_CONNECT_TIMEOUT, duration(config.connectTimeout))
      .withDuration(REQUEST_TIMEOUT, duration(config.readTimeout))
    steps.foldLeft(base) { (builder, step) => step(builder) }
  }

  private def authentication(builder: Builder, config: AuthenticationConfig): Builder = {
    builder
      .withString(AUTH_PROVIDER_CLASS, "PlainTextAuthProvider")
      .withString(AUTH_PROVIDER_USER_NAME, config.username)
      .withString(AUTH_PROVIDER_PASSWORD, config.password.value)
  }

  private def loadBalancing(config: Option[LoadBalancingConfig]): Builder => Builder = { builder =>
    config.map(_.localDc).filter(_.nonEmpty) match {
      case Some(localDc) => builder.withString(LOAD_BALANCING_LOCAL_DATACENTER, localDc)
      case None => builder.withString(LOAD_BALANCING_POLICY_CLASS, "DcInferringLoadBalancingPolicy")
    }
  }

  private def speculativeExecution(builder: Builder, config: SpeculativeExecutionConfig): Builder = {
    builder
      .withString(SPECULATIVE_EXECUTION_POLICY_CLASS, "ConstantSpeculativeExecutionPolicy")
      .withDuration(SPECULATIVE_EXECUTION_DELAY, duration(config.delay))
      .withInt(SPECULATIVE_EXECUTION_MAX, config.maxExecutions + 1)
  }

  private def logQueries(builder: Builder): Builder = {
    builder
      .withStringList(REQUEST_TRACKER_CLASSES, List("RequestLogger").asJava)
      .withBoolean(REQUEST_LOGGER_SUCCESS_ENABLED, true)
      .withBoolean(REQUEST_LOGGER_SLOW_ENABLED, true)
      .withBoolean(REQUEST_LOGGER_ERROR_ENABLED, true)
  }

  private def duration(value: FiniteDuration): DurationJ = DurationJ.ofNanos(value.toNanos)

  private def opt[A](value: Option[A])(f: (Builder, A) => Builder): Builder => Builder = { builder =>
    value.fold(builder)(f(builder, _))
  }

  private def when(condition: Boolean)(f: Builder => Builder): Builder => Builder = {
    if (condition) f else identity
  }
}

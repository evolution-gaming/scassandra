package com.evolution.scassandra4

import com.datastax.oss.driver.api.core.config.{DefaultDriverOption, DriverConfigLoader, ProgrammaticDriverConfigLoaderBuilder}

import java.time.{Duration => DurationJ}
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

/** Translates [[CassandraConfig]] (the driver 3 era schema) into driver 4's
  * own configuration.
  *
  * The mapping, driver 3 setting → driver 4 option:
  *   - `protocol-version` → `advanced.protocol.version`
  *   - `compression` → `advanced.protocol.compression`
  *   - `query.consistency` → `basic.request.consistency`
  *   - `query.serial-consistency` → `basic.request.serial-consistency`
  *   - `query.fetch-size` → `basic.request.page-size`
  *   - `query.default-idempotence` → `basic.request.default-idempotence`
  *   - `query.metadata` → `advanced.metadata.schema.enabled` and
  *     `advanced.metadata.token-map.enabled`
  *   - `query.refresh-schema-interval` → `advanced.metadata.schema.debouncer.window`
  *   - `query.max-pending-refresh-schema-requests` → `advanced.metadata.schema.debouncer.max-events`
  *   - `query.refresh-node-list-interval` → `advanced.metadata.topology-event-debouncer.window`
  *   - `query.max-pending-refresh-node-list-requests` → `advanced.metadata.topology-event-debouncer.max-events`
  *   - `query.re-prepare-on-up` → `advanced.prepared-statements.reprepare-on-up.enabled`
  *   - `query.prepare-on-all-hosts` → `advanced.prepared-statements.prepare-on-all-nodes`
  *   - `reconnection` → `advanced.reconnection-policy` (`ExponentialReconnectionPolicy`)
  *   - `socket.connect-timeout` → `advanced.connection.connect-timeout`
  *   - `socket.read-timeout` → `basic.request.timeout`
  *   - `socket.*` → `advanced.socket.*`
  *   - `pooling` → `advanced.connection.pool.*.size`,
  *     `advanced.connection.max-requests-per-connection`, `advanced.heartbeat.interval`
  *   - `authentication` → `advanced.auth-provider` (`PlainTextAuthProvider`)
  *   - `load-balancing.local-dc` → `basic.load-balancing-policy.local-datacenter`;
  *     when absent or empty, `DcInferringLoadBalancingPolicy` is used instead
  *   - `load-balancing.allow-remote-dcs-for-local-consistency-level` →
  *     `advanced.load-balancing-policy.dc-failover.allow-for-local-consistency-levels`
  *   - `speculative-execution` → `advanced.speculative-execution-policy`
  *     (`ConstantSpeculativeExecutionPolicy`; driver 4's `max-executions`
  *     includes the initial execution, hence the `+ 1`)
  *   - `log-queries` → `advanced.request-tracker` (`RequestLogger`)
  *
  * Ignored, no driver 4 counterpart: `jmx-reporting`, `metrics` (driver 4
  * metrics require a registry and are not translated yet),
  * `query.refresh-node-interval`, `query.max-pending-refresh-node-requests`,
  * `pooling.pool-timeout`, `pooling.idle-timeout`, `pooling.max-queue-size`,
  * `pooling.*.new-connection-threshold`, `pooling.*.connections-per-host-min`,
  * `pooling.remote.max-requests-per-connection`.
  */
object CreateDriverConfigLoader {

  def apply(config: CassandraConfig, sessionName: String): DriverConfigLoader = {

    def duration(value: FiniteDuration) = DurationJ.ofNanos(value.toNanos)

    val query = config.query
    val socket = config.socket
    val pooling = config.pooling
    val reconnection = config.reconnection

    val builder = DriverConfigLoader
      .programmaticBuilder()
      .withString(DefaultDriverOption.SESSION_NAME, sessionName)
      .withString(DefaultDriverOption.REQUEST_CONSISTENCY, query.consistency.name)
      .withString(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY, query.serialConsistency.name)
      .withInt(DefaultDriverOption.REQUEST_PAGE_SIZE, query.fetchSize)
      .withBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE, query.defaultIdempotence)
      .withBoolean(DefaultDriverOption.METADATA_SCHEMA_ENABLED, query.metadata)
      .withBoolean(DefaultDriverOption.METADATA_TOKEN_MAP_ENABLED, query.metadata)
      .withDuration(DefaultDriverOption.METADATA_SCHEMA_WINDOW, duration(query.refreshSchemaInterval))
      .withInt(DefaultDriverOption.METADATA_SCHEMA_MAX_EVENTS, query.maxPendingRefreshSchemaRequests)
      .withDuration(DefaultDriverOption.METADATA_TOPOLOGY_WINDOW, duration(query.refreshNodeListInterval))
      .withInt(DefaultDriverOption.METADATA_TOPOLOGY_MAX_EVENTS, query.maxPendingRefreshNodeListRequests)
      .withBoolean(DefaultDriverOption.REPREPARE_ENABLED, query.rePrepareOnUp)
      .withBoolean(DefaultDriverOption.PREPARE_ON_ALL_NODES, query.prepareOnAllHosts)
      .withString(DefaultDriverOption.RECONNECTION_POLICY_CLASS, "ExponentialReconnectionPolicy")
      .withDuration(DefaultDriverOption.RECONNECTION_BASE_DELAY, duration(reconnection.minDelay))
      .withDuration(DefaultDriverOption.RECONNECTION_MAX_DELAY, duration(reconnection.maxDelay))
      .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, duration(socket.connectTimeout))
      .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, duration(socket.readTimeout))
      .withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, pooling.local.connectionsPerHostMax)
      .withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, pooling.remote.connectionsPerHostMax)
      .withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS, pooling.local.maxRequestsPerConnection)
      .withDuration(DefaultDriverOption.HEARTBEAT_INTERVAL, duration(pooling.heartbeatInterval))

    val withSocket = {
      def set[A](
        builder: ProgrammaticDriverConfigLoaderBuilder,
        value: Option[A])(
        f: (ProgrammaticDriverConfigLoaderBuilder, A) => ProgrammaticDriverConfigLoaderBuilder,
      ) = {
        value.fold(builder) { value => f(builder, value) }
      }

      var result = builder
      result = set(result, socket.keepAlive) { (b, a) => b.withBoolean(DefaultDriverOption.SOCKET_KEEP_ALIVE, a) }
      result = set(result, socket.reuseAddress) { (b, a) => b.withBoolean(DefaultDriverOption.SOCKET_REUSE_ADDRESS, a) }
      result = set(result, socket.soLinger) { (b, a) => b.withInt(DefaultDriverOption.SOCKET_LINGER_INTERVAL, a) }
      result = set(result, socket.tcpNoDelay) { (b, a) => b.withBoolean(DefaultDriverOption.SOCKET_TCP_NODELAY, a) }
      result = set(result, socket.receiveBufferSize) { (b, a) => b.withInt(DefaultDriverOption.SOCKET_RECEIVE_BUFFER_SIZE, a) }
      result = set(result, socket.sendBufferSize) { (b, a) => b.withInt(DefaultDriverOption.SOCKET_SEND_BUFFER_SIZE, a) }
      result
    }

    val withProtocol = {
      val withVersion = config.protocolVersion.fold(withSocket) { version =>
        withSocket.withString(DefaultDriverOption.PROTOCOL_VERSION, version.name)
      }
      config.compression match {
        case Compression.None => withVersion
        case compression      => withVersion.withString(DefaultDriverOption.PROTOCOL_COMPRESSION, compression.name)
      }
    }

    val withAuthentication = config.authentication.fold(withProtocol) { authentication =>
      withProtocol
        .withString(DefaultDriverOption.AUTH_PROVIDER_CLASS, "PlainTextAuthProvider")
        .withString(DefaultDriverOption.AUTH_PROVIDER_USER_NAME, authentication.username)
        .withString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, authentication.password.value)
    }

    val withLoadBalancing = config.loadBalancing match {
      case Some(loadBalancing) if loadBalancing.localDc.nonEmpty =>
        withAuthentication
          .withString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER, loadBalancing.localDc)
          .withBoolean(
            DefaultDriverOption.LOAD_BALANCING_DC_FAILOVER_ALLOW_FOR_LOCAL_CONSISTENCY_LEVELS,
            loadBalancing.allowRemoteDcsForLocalConsistencyLevel)
      case _                                                     =>
        // the default load balancing policy of driver 4 refuses to start without
        // an explicit local datacenter, infer it from the contact points instead
        withAuthentication.withString(
          DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS,
          "DcInferringLoadBalancingPolicy")
    }

    val withSpeculativeExecution = config.speculativeExecution.fold(withLoadBalancing) { speculativeExecution =>
      withLoadBalancing
        .withString(DefaultDriverOption.SPECULATIVE_EXECUTION_POLICY_CLASS, "ConstantSpeculativeExecutionPolicy")
        .withDuration(DefaultDriverOption.SPECULATIVE_EXECUTION_DELAY, duration(speculativeExecution.delay))
        .withInt(DefaultDriverOption.SPECULATIVE_EXECUTION_MAX, speculativeExecution.maxExecutions + 1)
    }

    val withLogQueries = {
      if (config.logQueries) {
        withSpeculativeExecution
          .withStringList(DefaultDriverOption.REQUEST_TRACKER_CLASSES, List("RequestLogger").asJava)
          .withBoolean(DefaultDriverOption.REQUEST_LOGGER_SUCCESS_ENABLED, true)
          .withBoolean(DefaultDriverOption.REQUEST_LOGGER_ERROR_ENABLED, true)
      } else {
        withSpeculativeExecution
      }
    }

    withLogQueries.build()
  }
}

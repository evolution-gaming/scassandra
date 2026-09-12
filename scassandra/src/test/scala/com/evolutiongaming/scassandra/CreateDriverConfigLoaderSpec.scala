package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.config.DefaultDriverOption.*
import com.datastax.oss.driver.api.core.config.{DriverExecutionProfile, DriverOption}
import com.datastax.oss.driver.api.core.{DefaultConsistencyLevel, DefaultProtocolVersion}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.Duration
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class CreateDriverConfigLoaderSpec extends AnyFunSuite with Matchers {

  private def profile(config: CassandraConfig, sessionName: String = "name-1"): DriverExecutionProfile = {
    CreateDriverConfigLoader(config, sessionName).getInitialConfig.getDefaultProfile
  }

  private val defaults = profile(CassandraConfig.Default)

  test("session name") {
    profile(CassandraConfig.Default, "cluster-7").getString(SESSION_NAME) shouldEqual "cluster-7"
  }

  test("defaults") {
    defaults.getString(REQUEST_CONSISTENCY) shouldEqual "LOCAL_ONE"
    defaults.getString(REQUEST_SERIAL_CONSISTENCY) shouldEqual "SERIAL"
    defaults.getInt(REQUEST_PAGE_SIZE) shouldEqual 5000
    defaults.getBoolean(REQUEST_DEFAULT_IDEMPOTENCE) shouldEqual false
    defaults.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(12)
    defaults.getDuration(CONNECTION_CONNECT_TIMEOUT) shouldEqual Duration.ofSeconds(5)
    defaults.getInt(CONNECTION_POOL_LOCAL_SIZE) shouldEqual 1
    defaults.getInt(CONNECTION_POOL_REMOTE_SIZE) shouldEqual 1
    defaults.getInt(CONNECTION_MAX_REQUESTS) shouldEqual 1024
    defaults.getDuration(HEARTBEAT_INTERVAL) shouldEqual Duration.ofSeconds(30)
    defaults.getString(RECONNECTION_POLICY_CLASS) shouldEqual "ExponentialReconnectionPolicy"
    defaults.getDuration(RECONNECTION_BASE_DELAY) shouldEqual Duration.ofSeconds(1)
    defaults.getDuration(RECONNECTION_MAX_DELAY) shouldEqual Duration.ofMinutes(10)
    defaults.getBoolean(SOCKET_TCP_NODELAY) shouldEqual true
    defaults.isDefined(SOCKET_KEEP_ALIVE) shouldEqual false
    defaults.isDefined(PROTOCOL_VERSION) shouldEqual false
    defaults.isDefined(PROTOCOL_COMPRESSION) shouldEqual false
    defaults.isDefined(AUTH_PROVIDER_CLASS) shouldEqual false
    defaults.getString(LOAD_BALANCING_POLICY_CLASS) shouldEqual "DcInferringLoadBalancingPolicy"
    defaults.isDefined(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual false
    defaults.getString(SPECULATIVE_EXECUTION_POLICY_CLASS) shouldEqual "NoSpeculativeExecutionPolicy"
    defaults.isDefined(REQUEST_TRACKER_CLASSES) shouldEqual false
  }

  test("query") {
    val query = QueryConfig(
      consistency = DefaultConsistencyLevel.ALL,
      serialConsistency = DefaultConsistencyLevel.LOCAL_SERIAL,
      fetchSize = 1,
      defaultIdempotence = true,
      maxPendingRefreshNodeListRequests = 2,
      maxPendingRefreshSchemaRequests = 4,
      refreshNodeListInterval = 5.seconds,
      refreshSchemaInterval = 7.seconds,
      metadata = false,
      rePrepareOnUp = false,
      prepareOnAllHosts = false,
    )
    val options = profile(CassandraConfig(query = query))
    options.getString(REQUEST_CONSISTENCY) shouldEqual "ALL"
    options.getString(REQUEST_SERIAL_CONSISTENCY) shouldEqual "LOCAL_SERIAL"
    options.getInt(REQUEST_PAGE_SIZE) shouldEqual 1
    options.getBoolean(REQUEST_DEFAULT_IDEMPOTENCE) shouldEqual true
    options.getInt(METADATA_TOPOLOGY_MAX_EVENTS) shouldEqual 2
    options.getInt(METADATA_SCHEMA_MAX_EVENTS) shouldEqual 4
    options.getDuration(METADATA_TOPOLOGY_WINDOW) shouldEqual Duration.ofSeconds(5)
    options.getDuration(METADATA_SCHEMA_WINDOW) shouldEqual Duration.ofSeconds(7)
    options.getBoolean(METADATA_SCHEMA_ENABLED) shouldEqual false
    options.getBoolean(METADATA_TOKEN_MAP_ENABLED) shouldEqual false
    options.getBoolean(REPREPARE_ENABLED) shouldEqual false
    options.getBoolean(PREPARE_ON_ALL_NODES) shouldEqual false
  }

  test("socket") {
    val socket = SocketConfig(
      connectTimeout = 1.second,
      readTimeout = 2.seconds,
      keepAlive = Some(true),
      reuseAddress = Some(true),
      soLinger = Some(3),
      tcpNoDelay = Some(false),
      receiveBufferSize = Some(4),
      sendBufferSize = Some(5),
    )
    val options = profile(CassandraConfig(socket = socket))
    options.getDuration(CONNECTION_CONNECT_TIMEOUT) shouldEqual Duration.ofSeconds(1)
    options.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(2)
    options.getBoolean(SOCKET_KEEP_ALIVE) shouldEqual true
    options.getBoolean(SOCKET_REUSE_ADDRESS) shouldEqual true
    options.getInt(SOCKET_LINGER_INTERVAL) shouldEqual 3
    options.getBoolean(SOCKET_TCP_NODELAY) shouldEqual false
    options.getInt(SOCKET_RECEIVE_BUFFER_SIZE) shouldEqual 4
    options.getInt(SOCKET_SEND_BUFFER_SIZE) shouldEqual 5
  }

  test("pooling and reconnection") {
    val config = CassandraConfig(
      pooling = PoolingConfig(
        localSize = 2,
        remoteSize = 3,
        maxRequestsPerConnection = 4,
        heartbeatInterval = 5.seconds,
      ),
      reconnection = ReconnectionConfig(minDelay = 6.seconds, maxDelay = 7.seconds),
    )
    val options = profile(config)
    options.getInt(CONNECTION_POOL_LOCAL_SIZE) shouldEqual 2
    options.getInt(CONNECTION_POOL_REMOTE_SIZE) shouldEqual 3
    options.getInt(CONNECTION_MAX_REQUESTS) shouldEqual 4
    options.getDuration(HEARTBEAT_INTERVAL) shouldEqual Duration.ofSeconds(5)
    options.getDuration(RECONNECTION_BASE_DELAY) shouldEqual Duration.ofSeconds(6)
    options.getDuration(RECONNECTION_MAX_DELAY) shouldEqual Duration.ofSeconds(7)
  }

  test("protocol") {
    val options = profile(CassandraConfig(
      protocolVersion = Some(DefaultProtocolVersion.V4),
      compression = Compression.Lz4,
    ))
    options.getString(PROTOCOL_VERSION) shouldEqual "V4"
    options.getString(PROTOCOL_COMPRESSION) shouldEqual "lz4"
  }

  test("authentication") {
    val options = profile(CassandraConfig(authentication = Some(AuthenticationConfig("user", "pass"))))
    options.getString(AUTH_PROVIDER_CLASS) shouldEqual "PlainTextAuthProvider"
    options.getString(AUTH_PROVIDER_USER_NAME) shouldEqual "user"
    options.getString(AUTH_PROVIDER_PASSWORD) shouldEqual "pass"
  }

  test("load balancing") {
    val options = profile(CassandraConfig(loadBalancing = Some(LoadBalancingConfig(localDc = "dc1"))))
    options.getString(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual "dc1"
    options.getString(LOAD_BALANCING_POLICY_CLASS) shouldEqual "DefaultLoadBalancingPolicy"
    val inferred = profile(CassandraConfig(loadBalancing = Some(LoadBalancingConfig(localDc = ""))))
    inferred.getString(LOAD_BALANCING_POLICY_CLASS) shouldEqual "DcInferringLoadBalancingPolicy"
    inferred.isDefined(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual false
  }

  test("speculative execution") {
    val speculative = SpeculativeExecutionConfig(delay = 1.second, maxExecutions = 3)
    val options = profile(CassandraConfig(speculativeExecution = Some(speculative)))
    options.getString(SPECULATIVE_EXECUTION_POLICY_CLASS) shouldEqual "ConstantSpeculativeExecutionPolicy"
    options.getDuration(SPECULATIVE_EXECUTION_DELAY) shouldEqual Duration.ofSeconds(1)
    options.getInt(SPECULATIVE_EXECUTION_MAX) shouldEqual 4
  }

  test("log queries") {
    val options = profile(CassandraConfig(logQueries = true))
    options.getStringList(REQUEST_TRACKER_CLASSES) shouldEqual List("RequestLogger").asJava
    List[DriverOption](
      REQUEST_LOGGER_SUCCESS_ENABLED,
      REQUEST_LOGGER_SLOW_ENABLED,
      REQUEST_LOGGER_ERROR_ENABLED,
    )
      .map(options.getBoolean) shouldEqual List(true, true, true)
  }
}

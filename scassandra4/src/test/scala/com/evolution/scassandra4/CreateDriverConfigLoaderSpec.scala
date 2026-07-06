package com.evolution.scassandra4

import cats.data.NonEmptyList
import com.datastax.oss.driver.api.core.config.{DefaultDriverOption, DriverExecutionProfile}
import com.datastax.oss.driver.api.core.{DefaultConsistencyLevel, DefaultProtocolVersion}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.{Duration => DurationJ}
import scala.concurrent.duration._

class CreateDriverConfigLoaderSpec extends AnyFunSuite with Matchers {

  private def profileOf(config: CassandraConfig): DriverExecutionProfile = {
    CreateDriverConfigLoader(config, "name-1")
      .getInitialConfig
      .getDefaultProfile
  }

  test("translate default config") {
    val profile = profileOf(CassandraConfig.Default)

    profile.getString(DefaultDriverOption.SESSION_NAME) shouldEqual "name-1"
    profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY) shouldEqual "LOCAL_ONE"
    profile.getString(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY) shouldEqual "SERIAL"
    profile.getInt(DefaultDriverOption.REQUEST_PAGE_SIZE) shouldEqual 5000
    profile.getBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE) shouldEqual false
    profile.getDuration(DefaultDriverOption.REQUEST_TIMEOUT) shouldEqual DurationJ.ofSeconds(12)
    profile.getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT) shouldEqual DurationJ.ofSeconds(5)
    profile.getString(DefaultDriverOption.RECONNECTION_POLICY_CLASS) shouldEqual "ExponentialReconnectionPolicy"
    profile.getDuration(DefaultDriverOption.RECONNECTION_BASE_DELAY) shouldEqual DurationJ.ofSeconds(1)
    profile.getDuration(DefaultDriverOption.RECONNECTION_MAX_DELAY) shouldEqual DurationJ.ofMinutes(10)
    profile.getInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE) shouldEqual 4
    profile.getInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE) shouldEqual 4
    profile.getInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS) shouldEqual 32768
    profile.getDuration(DefaultDriverOption.HEARTBEAT_INTERVAL) shouldEqual DurationJ.ofSeconds(30)
    profile.getBoolean(DefaultDriverOption.SOCKET_TCP_NODELAY) shouldEqual true
    profile.getBoolean(DefaultDriverOption.METADATA_SCHEMA_ENABLED) shouldEqual true
    // no local datacenter configured, falls back to inferring it
    profile.getString(DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS) shouldEqual "DcInferringLoadBalancingPolicy"
    profile.isDefined(DefaultDriverOption.PROTOCOL_COMPRESSION) shouldEqual false
    profile.isDefined(DefaultDriverOption.PROTOCOL_VERSION) shouldEqual false
    profile.isDefined(DefaultDriverOption.AUTH_PROVIDER_CLASS) shouldEqual false
    profile.isDefined(DefaultDriverOption.SPECULATIVE_EXECUTION_DELAY) shouldEqual false
  }

  test("translate custom config") {
    val config = CassandraConfig(
      name = "name",
      contactPoints = NonEmptyList.of("127.0.0.1"),
      protocolVersion = Some(DefaultProtocolVersion.V4),
      query = QueryConfig(
        consistency = DefaultConsistencyLevel.ALL,
        serialConsistency = DefaultConsistencyLevel.LOCAL_SERIAL,
        fetchSize = 1,
        defaultIdempotence = true,
        metadata = false),
      socket = SocketConfig(
        connectTimeout = 1.second,
        readTimeout = 2.seconds,
        keepAlive = Some(true),
        soLinger = Some(3),
        receiveBufferSize = Some(4),
        sendBufferSize = Some(5)),
      authentication = Some(AuthenticationConfig("username", "password")),
      loadBalancing = Some(LoadBalancingConfig(
        localDc = "dc1",
        allowRemoteDcsForLocalConsistencyLevel = true)),
      speculativeExecution = Some(SpeculativeExecutionConfig(
        delay = 500.millis,
        maxExecutions = 2)),
      compression = Compression.Lz4,
      logQueries = true)

    val profile = profileOf(config)

    profile.getString(DefaultDriverOption.PROTOCOL_VERSION) shouldEqual "V4"
    profile.getString(DefaultDriverOption.PROTOCOL_COMPRESSION) shouldEqual "lz4"
    profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY) shouldEqual "ALL"
    profile.getString(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY) shouldEqual "LOCAL_SERIAL"
    profile.getInt(DefaultDriverOption.REQUEST_PAGE_SIZE) shouldEqual 1
    profile.getBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE) shouldEqual true
    profile.getBoolean(DefaultDriverOption.METADATA_SCHEMA_ENABLED) shouldEqual false
    profile.getBoolean(DefaultDriverOption.METADATA_TOKEN_MAP_ENABLED) shouldEqual false
    profile.getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT) shouldEqual DurationJ.ofSeconds(1)
    profile.getDuration(DefaultDriverOption.REQUEST_TIMEOUT) shouldEqual DurationJ.ofSeconds(2)
    profile.getBoolean(DefaultDriverOption.SOCKET_KEEP_ALIVE) shouldEqual true
    profile.getInt(DefaultDriverOption.SOCKET_LINGER_INTERVAL) shouldEqual 3
    profile.getInt(DefaultDriverOption.SOCKET_RECEIVE_BUFFER_SIZE) shouldEqual 4
    profile.getInt(DefaultDriverOption.SOCKET_SEND_BUFFER_SIZE) shouldEqual 5
    profile.getString(DefaultDriverOption.AUTH_PROVIDER_CLASS) shouldEqual "PlainTextAuthProvider"
    profile.getString(DefaultDriverOption.AUTH_PROVIDER_USER_NAME) shouldEqual "username"
    profile.getString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD) shouldEqual "password"
    profile.getString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual "dc1"
    profile.getBoolean(DefaultDriverOption.LOAD_BALANCING_DC_FAILOVER_ALLOW_FOR_LOCAL_CONSISTENCY_LEVELS) shouldEqual true
    profile.getString(DefaultDriverOption.SPECULATIVE_EXECUTION_POLICY_CLASS) shouldEqual "ConstantSpeculativeExecutionPolicy"
    profile.getDuration(DefaultDriverOption.SPECULATIVE_EXECUTION_DELAY) shouldEqual DurationJ.ofMillis(500)
    // driver 4 counts the initial execution, driver 3 counted only speculative ones
    profile.getInt(DefaultDriverOption.SPECULATIVE_EXECUTION_MAX) shouldEqual 3
    profile.getStringList(DefaultDriverOption.REQUEST_TRACKER_CLASSES) should contain ("RequestLogger")
    profile.getBoolean(DefaultDriverOption.REQUEST_LOGGER_SUCCESS_ENABLED) shouldEqual true
    profile.getBoolean(DefaultDriverOption.REQUEST_LOGGER_ERROR_ENABLED) shouldEqual true
  }
}

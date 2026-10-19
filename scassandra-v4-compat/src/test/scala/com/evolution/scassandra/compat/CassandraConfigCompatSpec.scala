package com.evolution.scassandra.compat

import com.datastax.driver.core.ProtocolVersion
import com.datastax.oss.driver.api.core.config.DefaultDriverOption.*
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile
import com.evolution.scassandra.compat.CassandraConfigCompatSpec.*
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.{CassandraConfig as CassandraConfigV3, PoolingConfig, QueryConfig}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource
import pureconfig.error.{ConfigReaderFailures, ConvertFailure}

import java.time.Duration
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class CassandraConfigCompatSpec extends AnyFunSuite with Matchers with EitherValues {

  test("empty config is translated with the defaults of scassandra for the driver 3") {
    val config = legacy("")
    val profile = profileOf(config)

    config.warnings shouldEqual List.empty
    profile.getString(SESSION_NAME) shouldEqual "cluster"
    profile.getStringList(CONTACT_POINTS).asScala shouldEqual List("127.0.0.1:9042")
    profile.isDefined(PROTOCOL_VERSION) shouldEqual false
    profile.isDefined(PROTOCOL_COMPRESSION) shouldEqual false
    profile.isDefined(CLOUD_SECURE_CONNECT_BUNDLE) shouldEqual false

    profile.getInt(CONNECTION_POOL_LOCAL_SIZE) shouldEqual 4
    profile.getInt(CONNECTION_POOL_REMOTE_SIZE) shouldEqual 4
    profile.getInt(CONNECTION_MAX_REQUESTS) shouldEqual 32767
    profile.getDuration(HEARTBEAT_INTERVAL) shouldEqual Duration.ofSeconds(30)

    profile.getString(REQUEST_CONSISTENCY) shouldEqual "LOCAL_ONE"
    profile.getString(REQUEST_SERIAL_CONSISTENCY) shouldEqual "SERIAL"
    profile.getInt(REQUEST_PAGE_SIZE) shouldEqual 5000
    profile.getBoolean(REQUEST_DEFAULT_IDEMPOTENCE) shouldEqual false
    profile.getDuration(METADATA_TOPOLOGY_WINDOW) shouldEqual Duration.ofSeconds(1)
    profile.getInt(METADATA_TOPOLOGY_MAX_EVENTS) shouldEqual 20
    profile.getDuration(METADATA_SCHEMA_WINDOW) shouldEqual Duration.ofSeconds(1)
    profile.getInt(METADATA_SCHEMA_MAX_EVENTS) shouldEqual 20
    profile.getBoolean(METADATA_SCHEMA_ENABLED) shouldEqual true
    profile.getBoolean(METADATA_TOKEN_MAP_ENABLED) shouldEqual true
    profile.getBoolean(REPREPARE_ENABLED) shouldEqual true
    profile.getBoolean(PREPARE_ON_ALL_NODES) shouldEqual true

    profile.getString(RECONNECTION_POLICY_CLASS) shouldEqual "ExponentialReconnectionPolicy"
    profile.getDuration(RECONNECTION_BASE_DELAY) shouldEqual Duration.ofSeconds(1)
    profile.getDuration(RECONNECTION_MAX_DELAY) shouldEqual Duration.ofMinutes(10)

    profile.getDuration(CONNECTION_CONNECT_TIMEOUT) shouldEqual Duration.ofSeconds(5)
    profile.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(12)
    profile.getBoolean(SOCKET_TCP_NODELAY) shouldEqual true
    profile.isDefined(SOCKET_KEEP_ALIVE) shouldEqual false

    profile.isDefined(AUTH_PROVIDER_CLASS) shouldEqual false
    profile.getString(LOAD_BALANCING_POLICY_CLASS) shouldEqual
      classOf[FirstContactPointDcLoadBalancingPolicy].getName
    profile.isDefined(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual false
    profile.getString(SPECULATIVE_EXECUTION_POLICY_CLASS) shouldEqual "NoSpeculativeExecutionPolicy"
    profile.getStringList(REQUEST_TRACKER_CLASSES, List.empty[String].asJava).asScala should not contain
      "RequestLogger"
    profile.getStringList(METRICS_SESSION_ENABLED).asScala shouldEqual List.empty
  }

  test("cluster.conf") {
    val config = legacyResource("cluster.conf")
    val profile = profileOf(config)

    config.warnings shouldEqual List.empty
    profile.getString(SESSION_NAME) shouldEqual "name"
    profile.getStringList(CONTACT_POINTS).asScala shouldEqual List("127.0.0.1:1", "127.0.0.2:1")
    profile.getString(PROTOCOL_VERSION) shouldEqual "V5"
    profile.getString(PROTOCOL_COMPRESSION) shouldEqual "lz4"
    profile.getString(AUTH_PROVIDER_CLASS) shouldEqual "PlainTextAuthProvider"
    profile.getString(AUTH_PROVIDER_USER_NAME) shouldEqual "username"
    profile.getString(AUTH_PROVIDER_PASSWORD) shouldEqual "password"
    profile.getString(LOAD_BALANCING_POLICY_CLASS) shouldEqual "DefaultLoadBalancingPolicy"
    profile.getString(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual "localDc"
    profile.getBoolean(LOAD_BALANCING_DC_FAILOVER_ALLOW_FOR_LOCAL_CONSISTENCY_LEVELS) shouldEqual false
    profile.getString(SPECULATIVE_EXECUTION_POLICY_CLASS) shouldEqual "ConstantSpeculativeExecutionPolicy"
    profile.getDuration(SPECULATIVE_EXECUTION_DELAY) shouldEqual Duration.ofMillis(500)
    profile.getInt(SPECULATIVE_EXECUTION_MAX) shouldEqual 3
    profile.getStringList(REQUEST_TRACKER_CLASSES).asScala shouldEqual List("RequestLogger")
    profile.getBoolean(REQUEST_LOGGER_SUCCESS_ENABLED) shouldEqual false
    profile.getBoolean(REQUEST_LOGGER_SLOW_ENABLED) shouldEqual true
    profile.getDuration(REQUEST_LOGGER_SLOW_THRESHOLD) shouldEqual Duration.ofSeconds(5)
    profile.getBoolean(REQUEST_LOGGER_ERROR_ENABLED) shouldEqual true
  }

  test("pooling.conf") {
    val config = legacyResource("pooling.conf", key = "pooling")
    val profile = profileOf(config)

    profile.getInt(CONNECTION_POOL_LOCAL_SIZE) shouldEqual 4
    profile.getInt(CONNECTION_POOL_REMOTE_SIZE) shouldEqual 8
    profile.getInt(CONNECTION_MAX_REQUESTS) shouldEqual 2
    profile.getDuration(HEARTBEAT_INTERVAL) shouldEqual Duration.ofHours(4)
    warnedKeys(config) shouldEqual Set(
      "cassandra.pooling.local.connections-per-host-min",
      "cassandra.pooling.remote.connections-per-host-min",
      "cassandra.pooling.remote.max-requests-per-connection",
      "cassandra.pooling.local.new-connection-threshold",
      "cassandra.pooling.remote.new-connection-threshold",
      "cassandra.pooling.pool-timeout",
      "cassandra.pooling.idle-timeout",
      "cassandra.pooling.max-queue-size",
    )
  }

  test("query.conf") {
    val config = legacyResource("query.conf", key = "query")
    val profile = profileOf(config)

    profile.getString(REQUEST_CONSISTENCY) shouldEqual "ALL"
    profile.getString(REQUEST_SERIAL_CONSISTENCY) shouldEqual "QUORUM"
    profile.getInt(REQUEST_PAGE_SIZE) shouldEqual 1
    profile.getBoolean(REQUEST_DEFAULT_IDEMPOTENCE) shouldEqual true
    profile.getDuration(METADATA_TOPOLOGY_WINDOW) shouldEqual Duration.ofMillis(5)
    profile.getInt(METADATA_TOPOLOGY_MAX_EVENTS) shouldEqual 2
    profile.getDuration(METADATA_SCHEMA_WINDOW) shouldEqual Duration.ofHours(7)
    profile.getInt(METADATA_SCHEMA_MAX_EVENTS) shouldEqual 4
    profile.getBoolean(METADATA_SCHEMA_ENABLED) shouldEqual false
    profile.getBoolean(METADATA_TOKEN_MAP_ENABLED) shouldEqual false
    profile.getBoolean(REPREPARE_ENABLED) shouldEqual false
    profile.getBoolean(PREPARE_ON_ALL_NODES) shouldEqual false
    warnedKeys(config) shouldEqual Set(
      "cassandra.query.refresh-node-interval",
      "cassandra.query.max-pending-refresh-node-requests",
    )
  }

  test("reconnection.conf") {
    val profile = profileOf(legacyResource("reconnection.conf", key = "reconnection"))

    profile.getDuration(RECONNECTION_BASE_DELAY) shouldEqual Duration.ofMillis(1)
    profile.getDuration(RECONNECTION_MAX_DELAY) shouldEqual Duration.ofSeconds(2)
  }

  test("socket.conf") {
    val profile = profileOf(legacyResource("socket.conf", key = "socket"))

    profile.getDuration(CONNECTION_CONNECT_TIMEOUT) shouldEqual Duration.ofMillis(1)
    profile.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(2)
    profile.getBoolean(SOCKET_KEEP_ALIVE) shouldEqual false
    profile.getBoolean(SOCKET_REUSE_ADDRESS) shouldEqual false
    profile.getInt(SOCKET_LINGER_INTERVAL) shouldEqual 3
    profile.getBoolean(SOCKET_TCP_NODELAY) shouldEqual false
    profile.getInt(SOCKET_RECEIVE_BUFFER_SIZE) shouldEqual 4
    profile.getInt(SOCKET_SEND_BUFFER_SIZE) shouldEqual 5
  }

  test("speculative-execution.conf counts the initial execution as well") {
    val profile = profileOf(legacyResource("speculative-execution.conf", key = "speculative-execution"))

    profile.getString(SPECULATIVE_EXECUTION_POLICY_CLASS) shouldEqual "ConstantSpeculativeExecutionPolicy"
    profile.getDuration(SPECULATIVE_EXECUTION_DELAY) shouldEqual Duration.ofMillis(1)
    profile.getInt(SPECULATIVE_EXECUTION_MAX) shouldEqual 4
  }

  test("load-balancing.conf") {
    val profile = profileOf(legacyResource("load-balancing.conf", key = "load-balancing"))

    profile.getString(LOAD_BALANCING_POLICY_CLASS) shouldEqual "DefaultLoadBalancingPolicy"
    profile.getString(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual "local"
    profile.getBoolean(LOAD_BALANCING_DC_FAILOVER_ALLOW_FOR_LOCAL_CONSISTENCY_LEVELS) shouldEqual true
  }

  test("empty local DC infers the local DC from the contact points") {
    val profile = profileOf(legacy("""load-balancing.local-dc = """""))

    profile.getString(LOAD_BALANCING_POLICY_CLASS) shouldEqual
      classOf[FirstContactPointDcLoadBalancingPolicy].getName
    profile.isDefined(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual false
  }

  test("authentication.conf") {
    val profile = profileOf(legacyResource("authentication.conf", key = "authentication"))

    profile.getString(AUTH_PROVIDER_CLASS) shouldEqual "PlainTextAuthProvider"
    profile.getString(AUTH_PROVIDER_USER_NAME) shouldEqual "username"
    profile.getString(AUTH_PROVIDER_PASSWORD) shouldEqual "password"
  }

  test("authentication without a password is skipped, as by the reader of the driver 3") {
    profileOf(legacy("""authentication.username = "username"""")).isDefined(AUTH_PROVIDER_CLASS) shouldEqual
      false
  }

  test("invalid value falls back to the defaults of its block, as by the reader of the driver 3") {
    val profile = profileOf(legacy("""query { consistency = most, fetch-size = 1 }"""))

    profile.getString(REQUEST_CONSISTENCY) shouldEqual "LOCAL_ONE"
    profile.getInt(REQUEST_PAGE_SIZE) shouldEqual 5000
  }

  test("contact points as a string") {
    val profile = profileOf(legacyResource("cluster_contact-points.conf"))

    profile.getStringList(CONTACT_POINTS).asScala shouldEqual List("127.0.0.1:9042", "127.0.0.2:9042")
  }

  test("contact points with ports") {
    val profile = profileOf(legacy("""port = 1, contact-points = ["host1:2", "host2"]"""))

    profile.getStringList(CONTACT_POINTS).asScala shouldEqual List("host1:2", "host2:1")
  }

  test("invalid contact points fail") {
    failurePaths(legacyFailure("""contact-points = ["host:port"]""")) shouldEqual
      List("cassandra.contact-points")
    failurePaths(legacyFailure("""contact-points = ["host:1:2"]""")) shouldEqual
      List("cassandra.contact-points")
    failurePaths(legacyFailure("""contact-points = ["host:70000"]""")) shouldEqual
      List("cassandra.contact-points")
    legacyFailure("""contact-points = []""").prettyPrint() should include("contact-points")
  }

  test("cloud secure connect bundle file replaces the contact points") {
    val profile = profileOf(legacyResource("cluster_cloud-secure-connect-bundle-file.conf"))

    profile.getString(CLOUD_SECURE_CONNECT_BUNDLE) shouldEqual "/my/path"
    profile.isDefined(CONTACT_POINTS) shouldEqual false
  }

  test("cloud secure connect bundle url replaces the contact points") {
    val profile = profileOf(legacyResource("cluster_cloud-secure-connect-bundle-url.conf"))

    profile.getString(CLOUD_SECURE_CONNECT_BUNDLE) shouldEqual "http://my.ws"
    profile.isDefined(CONTACT_POINTS) shouldEqual false
  }

  test("cloud secure connect bundle with empty values is ignored") {
    val profile = profileOf(legacyResource("cluster_cloud-secure-connect-bundle-empty-values.conf"))

    profile.isDefined(CLOUD_SECURE_CONNECT_BUNDLE) shouldEqual false
    profile.getStringList(CONTACT_POINTS).asScala shouldEqual List("127.0.0.1:9042")
  }

  test("cloud secure connect bundle prefers a non-empty value") {
    val profile = profileOf(legacyResource("cluster_cloud-secure-connect-bundle-partial-empty.conf"))

    profile.getString(CLOUD_SECURE_CONNECT_BUNDLE) shouldEqual "/my/path"
  }

  test("cloud secure connect bundle in the driver configuration replaces the contact points") {
    val profile = profileOf(legacy("""driver.basic.cloud.secure-connect-bundle = "/my/path""""))

    profile.getString(CLOUD_SECURE_CONNECT_BUNDLE) shouldEqual "/my/path"
    profile.isDefined(CONTACT_POINTS) shouldEqual false
  }

  test("driver configuration is applied on top of the translated one") {
    val config = legacy(
      """
        |socket.read-timeout = 1s
        |contact-points = ["host1"]
        |driver {
        |  basic.request.timeout = 3s
        |  basic.contact-points = ["host2:9043"]
        |  basic.load-balancing-policy.local-datacenter = dc1
        |}
        |""".stripMargin,
    )
    val profile = profileOf(config)

    profile.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(3)
    profile.getStringList(CONTACT_POINTS).asScala shouldEqual List("host2:9043")
    profile.getString(LOAD_BALANCING_LOCAL_DATACENTER) shouldEqual "dc1"
    profile.getString(REQUEST_CONSISTENCY) shouldEqual "LOCAL_ONE"
  }

  test("max requests per connection is limited by the driver 4 maximum") {
    def maxRequests(value: Int) = {
      profileOf(legacy(s"pooling.local.max-requests-per-connection = $value")).getInt(CONNECTION_MAX_REQUESTS)
    }

    maxRequests(32768) shouldEqual 32767
    maxRequests(100) shouldEqual 100
    failurePaths(legacyFailure("pooling.local.max-requests-per-connection = 32769")) shouldEqual
      List("cassandra.pooling.local.max-requests-per-connection")
  }

  test("protocol versions older than V3 fail") {
    failurePaths(legacyFailure("protocol-version = v2")) shouldEqual List("cassandra.protocol-version")
  }

  test("enum values are case insensitive") {
    val profile = profileOf(
      legacy("""protocol-version = v4, compression = Snappy, query.consistency = local_quorum"""),
    )

    profile.getString(PROTOCOL_VERSION) shouldEqual "V4"
    profile.getString(PROTOCOL_COMPRESSION) shouldEqual "snappy"
    profile.getString(REQUEST_CONSISTENCY) shouldEqual "LOCAL_QUORUM"
  }

  test("no compression") {
    profileOf(legacy("compression = none")).isDefined(PROTOCOL_COMPRESSION) shouldEqual false
  }

  test("translation failures are accumulated") {
    val failure = legacyFailure(
      """
        |protocol-version = v2
        |contact-points = ["host:port"]
        |pooling.local.max-requests-per-connection = 0
        |""".stripMargin,
    )

    failurePaths(failure).toSet shouldEqual Set(
      "cassandra.protocol-version",
      "cassandra.contact-points",
      "cassandra.pooling.local.max-requests-per-connection",
    )
  }

  test("metrics enable a fixed list of the driver metrics") {
    val profile = profileOf(legacy("metrics = true"))

    profile.getStringList(METRICS_SESSION_ENABLED).asScala shouldEqual V3Translation.SessionMetrics
    profile.getStringList(METRICS_NODE_ENABLED).asScala shouldEqual V3Translation.NodeMetrics
  }

  test("jmx reporting is ignored with a warning") {
    warnedKeys(legacy("jmx-reporting = true")) shouldEqual Set("cassandra.jmx-reporting")
  }

  test("ignored key set to its default produces no warning") {
    legacy("pooling.pool-timeout = 5s").warnings shouldEqual List.empty
  }

  test("legacyConfigReader at a nested path is the same as fromLegacyConfig") {
    val config = ConfigFactory.parseString("""a.b { name = "test", protocol-version = v2 }""")
    val reader = CassandraConfigCompat.legacyConfigReader

    failurePaths(ConfigSource.fromConfig(config).at("a.b").load(reader).left.value) shouldEqual
      List("a.b.protocol-version")
    failurePaths(CassandraConfigCompat.fromLegacyConfig(config, "a.b").left.value) shouldEqual
      List("a.b.protocol-version")
  }

  test("legacyConfigReader at the root") {
    val config = ConfigFactory.parseString("""name = "test"""")
    val result = ConfigSource.fromConfig(config).load(CassandraConfigCompat.legacyConfigReader).value

    profileOf(result).getString(SESSION_NAME) shouldEqual "test"
  }

  test("fromLegacyConfig fails if the path is missing") {
    CassandraConfigCompat.fromLegacyConfig(ConfigFactory.empty(), "cassandra").isLeft shouldEqual true
  }

  test("config with all the known keys produces warnings for the ignored keys only") {
    val fullConfig = ConfigFactory.parseString(FullLegacyConfig)
    val ignored = V3Translation.Keys.collect {
      case V3Translation.Key(path, V3Translation.Status.Ignored, _) => path
    }

    V3Translation.Keys.filterNot { key => fullConfig.hasPath(key.path) } shouldEqual List.empty
    V3Translation.IgnoredKeys.map { case (key, _) => key } should contain theSameElementsAs ignored
    warnedKeys(legacy(FullLegacyConfig)) shouldEqual ignored.map { key => s"cassandra.$key" }.toSet
  }

  test("fromV3 with the default config has no warnings") {
    CassandraConfigCompat.fromV3(CassandraConfigV3.Default).value.warnings shouldEqual List.empty
  }

  test("fromV3 produces warnings for the changed ignored fields only") {
    val config = CassandraConfigV3.Default.copy(
      pooling = PoolingConfig.Default.copy(maxQueueSize = 1),
      query = QueryConfig.Default.copy(refreshNodeInterval = 2.seconds),
    )

    warnedKeys(CassandraConfigCompat.fromV3(config).value) shouldEqual Set(
      "pooling.max-queue-size",
      "query.refresh-node-interval",
    )
  }

  test("fromV3 applies the driver overrides on top of the translated config") {
    val config = CassandraConfigV3.Default.copy(contactPoints = Nel("host1"))
    val overrides = ConfigFactory.parseString("basic.request.timeout = 3s")
    val profile = profileOf(CassandraConfigCompat.fromV3(config, overrides).value)

    profile.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(3)
    profile.getStringList(CONTACT_POINTS).asScala shouldEqual List("host1:9042")
  }

  test("reconnection policy can be replaced in the driver configuration") {
    val profile = profileOf(
      legacy(
        """
          |reconnection { min-delay = 2s, max-delay = 3min }
          |driver.advanced.reconnection-policy {
          |  class = ConstantReconnectionPolicy
          |  base-delay = 5s
          |}
          |""".stripMargin,
      ),
    )

    profile.getString(RECONNECTION_POLICY_CLASS) shouldEqual "ConstantReconnectionPolicy"
    profile.getDuration(RECONNECTION_BASE_DELAY) shouldEqual Duration.ofSeconds(5)
    // the driver configuration is merged key by key, so the translated keys it does not set stay
    profile.getDuration(RECONNECTION_MAX_DELAY) shouldEqual Duration.ofMinutes(3)
  }

  test("replacing only the reconnection policy class keeps the translated delay") {
    val profile = profileOf(
      legacy("""reconnection.min-delay = 2s, driver.advanced.reconnection-policy.class = ConstantReconnectionPolicy"""),
    )

    profile.getString(RECONNECTION_POLICY_CLASS) shouldEqual "ConstantReconnectionPolicy"
    profile.getDuration(RECONNECTION_BASE_DELAY) shouldEqual Duration.ofSeconds(2)
  }

  test("fromV3 can replace the reconnection policy with the driver overrides") {
    val overrides = ConfigFactory.parseString(
      "advanced.reconnection-policy { class = ConstantReconnectionPolicy, base-delay = 5s }",
    )
    val profile = profileOf(CassandraConfigCompat.fromV3(CassandraConfigV3.Default, overrides).value)

    profile.getString(RECONNECTION_POLICY_CLASS) shouldEqual "ConstantReconnectionPolicy"
    profile.getDuration(RECONNECTION_BASE_DELAY) shouldEqual Duration.ofSeconds(5)
    profile.getDuration(RECONNECTION_MAX_DELAY) shouldEqual Duration.ofMinutes(10)
  }

  test("fromV3 failures have relative paths") {
    val config = CassandraConfigV3.Default.copy(protocolVersion = Some(ProtocolVersion.V1))

    failurePaths(CassandraConfigCompat.fromV3(config).left.value) shouldEqual List("protocol-version")
  }

  test("fromLegacyConfig is the same as fromV3 of the config read by the driver 3 reader") {
    val hocon = fixtureConfig("cluster.conf")
    val v3 = ConfigSource.fromConfig(hocon).load[CassandraConfigV3].value

    CassandraConfigCompat.fromLegacyConfig(
      hocon.atKey("cassandra"),
      "cassandra",
    ).value.config.driver shouldEqual
      CassandraConfigCompat.fromV3(v3).value.config.driver
  }
}

object CassandraConfigCompatSpec {

  val FullLegacyConfig: String =
    """
      |name = "name"
      |port = 1
      |contact-points = ["host"]
      |protocol-version = V4
      |compression = LZ4
      |pooling {
      |  pool-timeout = 1s
      |  idle-timeout = 1s
      |  max-queue-size = 1
      |  heartbeat-interval = 1s
      |  local {
      |    new-connection-threshold = 1
      |    max-requests-per-connection = 1
      |    connections-per-host-min = 2
      |    connections-per-host-max = 2
      |  }
      |  remote {
      |    new-connection-threshold = 1
      |    max-requests-per-connection = 1
      |    connections-per-host-min = 2
      |    connections-per-host-max = 2
      |  }
      |}
      |query {
      |  consistency = ONE
      |  serial-consistency = LOCAL_SERIAL
      |  fetch-size = 1
      |  default-idempotence = true
      |  max-pending-refresh-node-list-requests = 1
      |  max-pending-refresh-node-requests = 1
      |  max-pending-refresh-schema-requests = 1
      |  refresh-node-list-interval = 2s
      |  refresh-node-interval = 2s
      |  refresh-schema-interval = 2s
      |  metadata = false
      |  re-prepare-on-up = false
      |  prepare-on-all-hosts = false
      |}
      |reconnection {
      |  min-delay = 1s
      |  max-delay = 2s
      |}
      |socket {
      |  connect-timeout = 1s
      |  read-timeout = 1s
      |  keep-alive = true
      |  reuse-address = true
      |  so-linger = 1
      |  tcp-no-delay = false
      |  receive-buffer-size = 1
      |  send-buffer-size = 1
      |}
      |authentication {
      |  username = "username"
      |  password = "password"
      |}
      |load-balancing {
      |  local-dc = "dc1"
      |  allow-remote-dcs-for-local-consistency-level = true
      |}
      |speculative-execution {
      |  delay = 1s
      |  max-executions = 1
      |}
      |log-queries = true
      |metrics = true
      |jmx-reporting = true
      |cloud-secure-connect-bundle {
      |  file = "/path"
      |  url = "http://url"
      |}
      |""".stripMargin

  // translates a v3 config given as HOCON, failing the test if the translation fails
  def legacy(hocon: String): TranslatedConfig = {
    legacyResult(ConfigFactory.parseString(hocon)).fold(
      failures => throw new AssertionError(failures.prettyPrint()),
      identity,
    )
  }

  // translates a v3 config given as HOCON, expecting the translation to fail
  def legacyFailure(hocon: String): ConfigReaderFailures = {
    legacyResult(ConfigFactory.parseString(hocon)).fold(identity, config => throw new AssertionError(config))
  }

  // reads a fixture copied from the test resources of scassandra for the driver 3
  def fixtureConfig(name: String): Config = {
    ConfigFactory.parseResources(classOf[CassandraConfigCompatSpec], name)
  }

  // translates a fixture; a fixture of one v3 section (e.g. `pooling.conf`) holds only the section contents,
  // so `key` names the section to wrap them under with `atKey` (it does not read from the file at `key`)
  def legacyResource(name: String, key: String = ""): TranslatedConfig = {
    val config = fixtureConfig(name)
    val section = if (key.isEmpty) config else config.atKey(key)
    legacyResult(section).fold(failures => throw new AssertionError(failures.prettyPrint()), identity)
  }

  // places the v3 config under `cassandra` and reads it from there, as a service would read it at its own path
  private def legacyResult(section: Config): Either[ConfigReaderFailures, TranslatedConfig] = {
    CassandraConfigCompat.fromLegacyConfig(section.atKey("cassandra"), "cassandra")
  }

  // the driver 4 settings as the driver sees them, with its `reference.conf` defaults applied
  def profileOf(translated: TranslatedConfig): DriverExecutionProfile = {
    translated.config.configLoader.getInitialConfig.getDefaultProfile
  }

  // the config paths the failures point at
  def failurePaths(failures: ConfigReaderFailures): List[String] = {
    failures.toList.flatMap {
      case failure: ConvertFailure => List(failure.path)
      case failure => failure.origin.map(_.description()).toList
    }
  }

  // the keys the warnings are about, quoted in backticks at the start of each warning
  def warnedKeys(config: TranslatedConfig): Set[String] = {
    config.warnings.map { warning => warning.drop(1).takeWhile(_ != '`') }.toSet
  }
}

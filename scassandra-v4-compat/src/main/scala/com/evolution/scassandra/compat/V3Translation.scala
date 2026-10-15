package com.evolution.scassandra.compat

import cats.Semigroup
import cats.syntax.all.*
import com.datastax.driver.core.ProtocolOptions.Compression
import com.datastax.oss.driver.api.core.DefaultProtocolVersion
import com.datastax.oss.driver.api.core.config.{DefaultDriverOption, DriverOption}
import com.evolutiongaming.scassandra.{
  CassandraConfig as CassandraConfigV3,
  CloudSecureConnectBundleConfig,
  PoolingConfig,
  QueryConfig,
}
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.error.{CannotConvert, ConfigReaderFailures, ConvertFailure, FailureReason}

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

/**
 * Translates the configuration of scassandra for the Java driver 3
 * (`com.evolutiongaming:scassandra`) into the configuration of the Java driver 4.
 *
 * See `docs/legacy-config-translation.md` for the details on how each key is translated.
 */
private[compat] object V3Translation {

  /**
   * How a key of the legacy configuration is translated.
   */
  sealed abstract class Status extends Product with Serializable

  object Status {

    /**
     * Has a driver 4 option with the same meaning
     */
    case object Exact extends Status

    /**
     * Has a driver 4 option with a different meaning, see the key note
     */
    case object Approximated extends Status

    /**
     * Has no driver 4 equivalent and is ignored with a warning
     */
    case object Ignored extends Status
  }

  /**
   * A key of the legacy configuration, relative to the configuration root.
   */
  final case class Key(path: String, status: Status, note: String = "")

  /**
   * All the keys of the legacy configuration known to the translation.
   *
   * Used to produce the warnings, and to check that `docs/legacy-config-translation.md`
   * is up to date.
   */
  val Keys: List[Key] = {
    import Status.*
    List(
      Key("name", Approximated, "becomes the session name, used as a prefix of logs and metrics"),
      Key("port", Exact),
      Key("contact-points", Exact),
      Key("cloud-secure-connect-bundle.file", Exact),
      Key("cloud-secure-connect-bundle.url", Exact),
      Key("protocol-version", Exact),
      Key("compression", Exact),
      Key("pooling.local.connections-per-host-max", Approximated, "becomes a fixed pool size"),
      Key("pooling.remote.connections-per-host-max", Approximated, "becomes a fixed pool size"),
      Key("pooling.local.max-requests-per-connection", Approximated, "applies to all the connections"),
      Key("pooling.heartbeat-interval", Exact),
      Key("pooling.local.connections-per-host-min", Ignored, "the pool has a fixed size"),
      Key("pooling.remote.connections-per-host-min", Ignored, "the pool has a fixed size"),
      Key(
        "pooling.remote.max-requests-per-connection",
        Ignored,
        "`pooling.local.max-requests-per-connection` applies to all the connections",
      ),
      Key("pooling.local.new-connection-threshold", Ignored, "the pool has a fixed size"),
      Key("pooling.remote.new-connection-threshold", Ignored, "the pool has a fixed size"),
      Key(
        "pooling.pool-timeout",
        Ignored,
        "requests are not queued by the pool, use `advanced.throttler` to limit the concurrency",
      ),
      Key("pooling.idle-timeout", Ignored, "idle connections are checked with heartbeats"),
      Key(
        "pooling.max-queue-size",
        Ignored,
        "requests are not queued by the pool, use `advanced.throttler` to limit the concurrency",
      ),
      Key("query.consistency", Exact),
      Key("query.serial-consistency", Exact),
      Key("query.fetch-size", Exact),
      Key("query.default-idempotence", Exact),
      Key("query.refresh-node-list-interval", Approximated, "applies to all the topology events"),
      Key("query.max-pending-refresh-node-list-requests", Approximated, "applies to all the topology events"),
      Key(
        "query.refresh-node-interval",
        Ignored,
        "`query.refresh-node-list-interval` applies to all the topology events",
      ),
      Key(
        "query.max-pending-refresh-node-requests",
        Ignored,
        "`query.max-pending-refresh-node-list-requests` applies to all the topology events",
      ),
      Key("query.refresh-schema-interval", Exact),
      Key("query.max-pending-refresh-schema-requests", Exact),
      Key("query.metadata", Approximated, "enables or disables both the schema and the token map metadata"),
      Key("query.re-prepare-on-up", Exact),
      Key("query.prepare-on-all-hosts", Exact),
      Key("reconnection.min-delay", Exact),
      Key("reconnection.max-delay", Exact),
      Key("socket.connect-timeout", Exact),
      Key(
        "socket.read-timeout",
        Approximated,
        "becomes the timeout of a whole request, including retries and speculative executions",
      ),
      Key("socket.keep-alive", Exact),
      Key("socket.reuse-address", Exact),
      Key("socket.so-linger", Exact),
      Key("socket.tcp-no-delay", Exact),
      Key("socket.receive-buffer-size", Exact),
      Key("socket.send-buffer-size", Exact),
      Key("authentication.username", Exact),
      Key("authentication.password", Exact),
      Key("load-balancing.local-dc", Exact),
      Key(
        "load-balancing.allow-remote-dcs-for-local-consistency-level",
        Approximated,
        "is applied, while it was ignored by scassandra for the Java driver 3",
      ),
      Key("speculative-execution.delay", Exact),
      Key("speculative-execution.max-executions", Approximated, "the initial execution is counted as well"),
      Key("log-queries", Approximated, "enables the request logger of the driver"),
      Key("metrics", Approximated, "enables a fixed list of the driver metrics"),
      Key(
        "jmx-reporting",
        Ignored,
        "the driver does not report metrics to JMX, use the metrics registry of the session instead",
      ),
    )
  }

  val SessionMetrics: List[String] = List(
    "bytes-sent",
    "bytes-received",
    "connected-nodes",
    "cql-requests",
    "cql-client-timeouts",
  )

  val NodeMetrics: List[String] = List(
    "pool.open-connections",
    "pool.in-flight",
    "bytes-sent",
    "bytes-received",
    "errors.connection.init",
    "errors.request.read-timeouts",
    "errors.request.write-timeouts",
    "errors.request.unavailables",
    "errors.request.others",
    "retries.total",
    "ignores.total",
    "speculative-executions",
  )

  /**
   * The ignored keys, producing a warning if the value differs from the default of
   * scassandra for the driver 3
   */
  val IgnoredKeys: List[(String, CassandraConfigV3 => Boolean)] = {
    val pooling = PoolingConfig.Default
    val query = QueryConfig.Default
    List(
      "pooling.local.connections-per-host-min" -> {
        _.pooling.local.connectionsPerHostMin != pooling.local.connectionsPerHostMin
      },
      "pooling.remote.connections-per-host-min" -> {
        _.pooling.remote.connectionsPerHostMin != pooling.remote.connectionsPerHostMin
      },
      "pooling.remote.max-requests-per-connection" -> {
        _.pooling.remote.maxRequestsPerConnection != pooling.remote.maxRequestsPerConnection
      },
      "pooling.local.new-connection-threshold" -> {
        _.pooling.local.newConnectionThreshold != pooling.local.newConnectionThreshold
      },
      "pooling.remote.new-connection-threshold" -> {
        _.pooling.remote.newConnectionThreshold != pooling.remote.newConnectionThreshold
      },
      "pooling.pool-timeout" -> { _.pooling.poolTimeout != pooling.poolTimeout },
      "pooling.idle-timeout" -> { _.pooling.idleTimeout != pooling.idleTimeout },
      "pooling.max-queue-size" -> { _.pooling.maxQueueSize != pooling.maxQueueSize },
      "query.refresh-node-interval" -> { _.query.refreshNodeInterval != query.refreshNodeInterval },
      "query.max-pending-refresh-node-requests" -> {
        _.query.maxPendingRefreshNodeRequests != query.maxPendingRefreshNodeRequests
      },
      "jmx-reporting" -> { _.jmxReporting != CassandraConfigV3.Default.jmxReporting },
    )
  }

  final case class Translated(driver: Config, warnings: List[String])

  /**
   * @param overrides
   *   the driver configuration, which will be applied on top of the translated one, used
   *   to resolve the conflicts, e.g. to skip the contact points if the cloud secure
   *   connect bundle is configured there
   * @param path
   *   path to the legacy configuration, if read from HOCON, used in the failures and the
   *   warnings
   */
  def apply(
    config: CassandraConfigV3,
    overrides: Config,
    path: Option[String],
  ): Either[ConfigReaderFailures, Translated] = {
    new Translation(config, overrides, path).translate
  }

  /**
   * The maximum value of `advanced.connection.max-requests-per-connection` accepted by
   * the driver 4, while the driver 3 accepted 32768 for the protocol V3 and newer
   */
  private val MaxRequestsPerConnectionLimit = 32767

  private val MaxRequestsPerConnectionV3 = 32768

  private type Result[A] = Either[ConfigReaderFailures, A]

  private type Entries = List[(DriverOption, AnyRef)]

  // to accumulate the failures
  private implicit val semigroupConfigReaderFailures: Semigroup[ConfigReaderFailures] = _ ++ _

  private final class Translation(config: CassandraConfigV3, overrides: Config, path: Option[String]) {

    private def fullPath(key: String): String = path.fold(key) { path => s"$path.$key" }

    private def failure(key: String, reason: FailureReason): ConfigReaderFailures = {
      ConfigReaderFailures(ConvertFailure(reason, origin = None, path = fullPath(key)))
    }

    def translate: Result[Translated] = {
      (contactPointsOrBundle, protocolVersion, maxRequestsPerConnection).parMapN {
        (contactPointsOrBundle, protocolVersion, maxRequestsPerConnection) =>
          val entries = List(
            contactPointsOrBundle,
            protocolVersion,
            maxRequestsPerConnection,
            basic,
            pooling,
            query,
            reconnection,
            socket,
            authentication,
            loadBalancing,
            speculativeExecution,
            requestLogger,
            metrics,
          ).flatten
          val values = entries.map { case (option, value) => option.getPath -> value }.toMap
          Translated(ConfigFactory.parseMap(values.asJava), warnings)
      }
    }

    private def warnings: List[String] = {
      IgnoredKeys.collect {
        case (key, differs) if differs(config) =>
          val note = Keys.find(_.path == key).fold("")(_.note)
          s"`${ fullPath(key) }` has no equivalent in the Java driver 4 and is ignored: $note"
      }
    }

    private def contactPointsOrBundle: Result[Entries] = {
      val bundleInOverrides = overrides.hasPath(DefaultDriverOption.CLOUD_SECURE_CONNECT_BUNDLE.getPath)
      config.cloudSecureConnectBundle match {
        case Some(CloudSecureConnectBundleConfig.File(file)) =>
          List(DefaultDriverOption.CLOUD_SECURE_CONNECT_BUNDLE -> file).asRight
        case Some(CloudSecureConnectBundleConfig.Url(url)) =>
          List(DefaultDriverOption.CLOUD_SECURE_CONNECT_BUNDLE -> url).asRight
        case None if bundleInOverrides =>
          List.empty.asRight
        case None =>
          config.contactPoints.toList
            .parTraverse { contactPoint =>
              parseContactPoint(contactPoint).leftMap(failure("contact-points", _))
            }
            .map { contactPoints => List(DefaultDriverOption.CONTACT_POINTS -> contactPoints.asJava) }
      }
    }

    // same as scassandra for the driver 3 did when creating a cluster
    private def parseContactPoint(contactPoint: String): Either[FailureReason, String] = {
      def invalid(because: String): FailureReason = CannotConvert(contactPoint, "contact point", because)

      def validPort(port: Int): Either[FailureReason, Int] = {
        if (port >= 0 && port <= 65535) port.asRight
        else invalid(s"port should be between 0 and 65535, but is $port").asLeft
      }

      def parsePort(port: String): Either[FailureReason, Int] = {
        port.toIntOption.toRight(invalid(s"port should be a number, but is $port")).flatMap(validPort)
      }

      contactPoint.split(":").map(_.trim) match {
        case Array(host, port) => parsePort(port).map { port => s"$host:$port" }
        case Array(host) => validPort(config.port).map { port => s"$host:$port" }
        case _ => invalid("should be in form of [host:port] or [host]").asLeft
      }
    }

    private def protocolVersion: Result[Entries] = {
      val supported = DefaultProtocolVersion.values().toList.map(_.name)
      config.protocolVersion.toList.parTraverse { version =>
        supported
          .find(_ == version.name)
          .map { version => DefaultDriverOption.PROTOCOL_VERSION -> version }
          .toRight {
            val reason = CannotConvert(
              version.name,
              "ProtocolVersion",
              s"the Java driver 4 supports: ${ supported.mkString(", ") }",
            )
            failure("protocol-version", reason)
          }
      }
    }

    private def maxRequestsPerConnection: Result[Entries] = {
      val maxRequests = config.pooling.local.maxRequestsPerConnection
      if (maxRequests >= 1 && maxRequests <= MaxRequestsPerConnectionV3) {
        val limited = maxRequests.min(MaxRequestsPerConnectionLimit)
        List(DefaultDriverOption.CONNECTION_MAX_REQUESTS -> Int.box(limited)).asRight
      } else {
        val reason = CannotConvert(
          maxRequests.toString,
          "max requests per connection",
          s"should be between 1 and $MaxRequestsPerConnectionV3",
        )
        failure("pooling.local.max-requests-per-connection", reason).asLeft
      }
    }

    private def basic: Entries = {
      val compression = config.compression match {
        case Compression.NONE => List.empty
        case compression => List(DefaultDriverOption.PROTOCOL_COMPRESSION -> compression.name.toLowerCase)
      }
      (DefaultDriverOption.SESSION_NAME -> config.name) :: compression
    }

    // TODO: discuss if connections-per-host-max is the right value for the fixed pool size
    private def pooling: Entries = List(
      DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE -> Int.box(config.pooling.local.connectionsPerHostMax),
      DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE -> Int.box(config.pooling.remote.connectionsPerHostMax),
      DefaultDriverOption.HEARTBEAT_INTERVAL -> duration(config.pooling.heartbeatInterval),
    )

    private def query: Entries = {
      val query = config.query
      List(
        DefaultDriverOption.REQUEST_CONSISTENCY -> query.consistency.name,
        DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY -> query.serialConsistency.name,
        DefaultDriverOption.REQUEST_PAGE_SIZE -> Int.box(query.fetchSize),
        DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE -> Boolean.box(query.defaultIdempotence),
        DefaultDriverOption.METADATA_TOPOLOGY_WINDOW -> duration(query.refreshNodeListInterval),
        DefaultDriverOption.METADATA_TOPOLOGY_MAX_EVENTS -> Int.box(query.maxPendingRefreshNodeListRequests),
        DefaultDriverOption.METADATA_SCHEMA_WINDOW -> duration(query.refreshSchemaInterval),
        DefaultDriverOption.METADATA_SCHEMA_MAX_EVENTS -> Int.box(query.maxPendingRefreshSchemaRequests),
        DefaultDriverOption.METADATA_SCHEMA_ENABLED -> Boolean.box(query.metadata),
        DefaultDriverOption.METADATA_TOKEN_MAP_ENABLED -> Boolean.box(query.metadata),
        DefaultDriverOption.REPREPARE_ENABLED -> Boolean.box(query.rePrepareOnUp),
        DefaultDriverOption.PREPARE_ON_ALL_NODES -> Boolean.box(query.prepareOnAllHosts),
      )
    }

    private def reconnection: Entries = List(
      DefaultDriverOption.RECONNECTION_POLICY_CLASS -> "ExponentialReconnectionPolicy",
      DefaultDriverOption.RECONNECTION_BASE_DELAY -> duration(config.reconnection.minDelay),
      DefaultDriverOption.RECONNECTION_MAX_DELAY -> duration(config.reconnection.maxDelay),
    )

    private def socket: Entries = {
      val socket = config.socket
      List(
        (DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT -> duration(socket.connectTimeout)).some,
        (DefaultDriverOption.REQUEST_TIMEOUT -> duration(socket.readTimeout)).some,
        socket.keepAlive.map { keepAlive => DefaultDriverOption.SOCKET_KEEP_ALIVE -> Boolean.box(keepAlive) },
        socket.reuseAddress.map { reuse => DefaultDriverOption.SOCKET_REUSE_ADDRESS -> Boolean.box(reuse) },
        socket.soLinger.map { linger => DefaultDriverOption.SOCKET_LINGER_INTERVAL -> Int.box(linger) },
        // `None` stands for the default of the driver, which is `true` in both drivers
        socket.tcpNoDelay.map { tcpNoDelay =>
          DefaultDriverOption.SOCKET_TCP_NODELAY -> Boolean.box(tcpNoDelay)
        },
        socket.receiveBufferSize.map { size =>
          DefaultDriverOption.SOCKET_RECEIVE_BUFFER_SIZE -> Int.box(size)
        },
        socket.sendBufferSize.map { size => DefaultDriverOption.SOCKET_SEND_BUFFER_SIZE -> Int.box(size) },
      ).flatten
    }

    private def authentication: Entries = {
      config.authentication.toList.flatMap { authentication =>
        List(
          DefaultDriverOption.AUTH_PROVIDER_CLASS -> "PlainTextAuthProvider",
          DefaultDriverOption.AUTH_PROVIDER_USER_NAME -> authentication.username,
          DefaultDriverOption.AUTH_PROVIDER_PASSWORD -> authentication.password.value,
        )
      }
    }

    private def loadBalancing: Entries = {
      config.loadBalancing match {
        case Some(loadBalancing) =>
          val allowRemoteDcs = Boolean.box(loadBalancing.allowRemoteDcsForLocalConsistencyLevel)
          val policy = if (loadBalancing.localDc.nonEmpty) {
            List(
              DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS -> "DefaultLoadBalancingPolicy",
              DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER -> loadBalancing.localDc,
            )
          } else {
            inferLocalDc
          }
          (DefaultDriverOption.LOAD_BALANCING_DC_FAILOVER_ALLOW_FOR_LOCAL_CONSISTENCY_LEVELS ->
            allowRemoteDcs) :: policy
        case None =>
          inferLocalDc
      }
    }

    // the driver 3 inferred the local DC from the contact points, while the default policy of
    // the driver 4 requires it to be configured explicitly
    private def inferLocalDc: Entries = List(
      DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS -> "DcInferringLoadBalancingPolicy",
    )

    private def speculativeExecution: Entries = {
      config.speculativeExecution.toList.flatMap { speculativeExecution =>
        List(
          DefaultDriverOption.SPECULATIVE_EXECUTION_POLICY_CLASS -> "ConstantSpeculativeExecutionPolicy",
          DefaultDriverOption.SPECULATIVE_EXECUTION_DELAY -> duration(speculativeExecution.delay),
          // the driver 3 counted the speculative executions only,
          // while the driver 4 counts the initial execution as well
          DefaultDriverOption.SPECULATIVE_EXECUTION_MAX -> Int.box(speculativeExecution.maxExecutions + 1),
        )
      }
    }

    private def requestLogger: Entries = {
      if (config.logQueries) {
        List(
          DefaultDriverOption.REQUEST_TRACKER_CLASSES -> List("RequestLogger").asJava,
          DefaultDriverOption.REQUEST_LOGGER_SUCCESS_ENABLED -> Boolean.box(true),
          DefaultDriverOption.REQUEST_LOGGER_SLOW_ENABLED -> Boolean.box(true),
          // the default threshold of the driver 3
          DefaultDriverOption.REQUEST_LOGGER_SLOW_THRESHOLD -> duration(5.seconds),
          DefaultDriverOption.REQUEST_LOGGER_ERROR_ENABLED -> Boolean.box(true),
        )
      } else {
        List.empty
      }
    }

    private def metrics: Entries = {
      if (config.metrics) {
        List(
          DefaultDriverOption.METRICS_SESSION_ENABLED -> SessionMetrics.asJava,
          DefaultDriverOption.METRICS_NODE_ENABLED -> NodeMetrics.asJava,
        )
      } else {
        List.empty
      }
    }

    private def duration(value: FiniteDuration): String = s"${ value.toMillis } milliseconds"
  }
}

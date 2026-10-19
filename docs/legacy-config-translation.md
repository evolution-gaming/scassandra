# Translation of the legacy configuration

`scassandra-v4-compat` translates the configuration of scassandra for the Java driver 3
(`com.evolutiongaming:scassandra`, referred to as the *legacy configuration* below) into the configuration of the
Java driver 4. This allows keeping the existing configuration as is, while moving to the configuration of the
driver 4 gradually.

`CassandraConfigCompat` provides:
* `fromV3(config, driverOverrides)` – translates the `com.evolutiongaming.scassandra.CassandraConfig` case
  class, with the configuration of the driver 4 applied on top;
* `fromLegacyConfig(config, path)` and `legacyConfigReader` – read the legacy configuration from HOCON with the
  reader of scassandra for the driver 3, together with the configuration of the driver 4 under the `driver`
  key, and translate it with `fromV3`.

Both produce the same driver configuration for the same input, as the HOCON is interpreted by the reader of
scassandra for the driver 3 in both cases.

## How the translation works

* **Precedence.** The configuration under the `driver` key of the legacy configuration is applied on top of the
  translated one, and the [`reference.conf`](https://github.com/apache/cassandra-java-driver/blob/4.x/core/src/main/resources/reference.conf)
  of the driver provides the remaining options:
  ```
  <path>.driver  >  translated <path>  >  reference.conf of the driver
  ```
  ```hocon
  my-service.cassandra {
    # legacy configuration, kept as is
    contact-points = ["host1", "host2"]
    query.consistency = "LOCAL_QUORUM"

    # configuration of the driver 4, wins over the translated one
    driver {
      basic.request.timeout = 3 seconds
      basic.load-balancing-policy.local-datacenter = "dc1"
    }
  }
  ```
  Once all the keys are moved to `driver`, read it directly with
  `CassandraConfig.fromConfig(config, "my-service.cassandra.driver")` from `scassandra-v4`, and drop the
  `scassandra-v4-compat` dependency. Mind that `fromConfig` applies the defaults of the driver 4 to the options
  missing under `driver`, rather than the defaults of scassandra for the driver 3 (next point): set every option
  which should keep its previous value, e.g. `basic.request.timeout = 12 seconds`.
* **The defaults of scassandra for the driver 3 are applied to the missing keys**, so an untouched configuration
  behaves as before, not as the plain driver 4. For example, the request timeout is 12 seconds rather than the
  2 seconds default of the driver 4. Use `CassandraConfig.fromConfig` for a configuration of the driver 4 only,
  to avoid inheriting these defaults.
* **Values are interpreted by the reader of scassandra for the driver 3**, exactly as before, including its
  leniency: an invalid value is not reported, the defaults are used for the whole block containing it instead.
  For example, `query { consistency = "MOST", fetch-size = 100 }` results in the default consistency *and* the
  default fetch size. An `authentication` block without a password results in no authentication.
* **Values not supported by the driver 4 fail** with `ConfigReaderFailures`, reporting all of them at once:
  protocol versions `V1` and `V2`, `pooling.local.max-requests-per-connection` outside of `1..32768`, and
  malformed contact points (the driver 3 failed on them when creating a cluster). The failures of
  `fromLegacyConfig` contain the full path of the key, the failures of `fromV3` contain the key only.
* **Ignored keys produce warnings**, returned as `TranslatedConfig.warnings` next to the translated
  `TranslatedConfig.config`; they are not logged, it is up to the caller. A warning is produced only if the value
  differs from the default of scassandra for the driver 3, so an ignored key explicitly set to its default produces
  no warning.
* **Unknown keys are ignored** without warnings, same as before.
* **`scassandra-v4-compat` depends on scassandra for the driver 3**, so the driver 3 and the versions of
  Netty, Guava and Jackson pinned by it are on the classpath. The driver 4 is tested to work with them.

## Keys

Status:
* **Exact** – the same meaning and value;
* **Approximated** – translated, but the meaning differs, see the notes;
* **Ignored** – no equivalent in the driver 4, the key is ignored with a warning.

| Legacy key | Legacy default | Driver 4 option | Status | Notes |
|---|---|---|---|---|
| `name` | `cluster` | `basic.session-name` | Approximated | Used as a prefix of the driver logs and metrics. scassandra for the driver 3 used `<name>-<id>` as the cluster name, the cluster name is now read from the server: `session.metadata.map(_.getClusterName)`. |
| `port` | `9042` | `basic.contact-points` | Exact | Appended to the contact points without a port. |
| `contact-points` | `["127.0.0.1"]` | `basic.contact-points` | Exact | A list or a string, separated by `,` or `;`. Not set if a cloud secure connect bundle is configured, either in the legacy or in the driver configuration. |
| `cloud-secure-connect-bundle.file` | not set | `basic.cloud.secure-connect-bundle` | Exact | An empty value is treated as not set. Preferred over `url`. |
| `cloud-secure-connect-bundle.url` | not set | `basic.cloud.secure-connect-bundle` | Exact | An empty value is treated as not set. |
| `protocol-version` | negotiated | `advanced.protocol.version` | Exact | Case insensitive. `V1` and `V2` are rejected, as the driver 4 supports `V3` and newer. |
| `compression` | `NONE` | `advanced.protocol.compression` | Exact | Case insensitive. `lz4` requires `at.yawk.lz4:lz4-java` and `snappy` requires `org.xerial.snappy:snappy-java` on the classpath, the driver 4 does not bring them. `snappy` is not supported by the protocol `V5` and newer. |
| `pooling.local.connections-per-host-max` | `4` | `advanced.connection.pool.local.size` | Approximated | The pool of the driver 4 has a fixed size, the maximum is used to keep the capacity. TODO: to be discussed, the driver 4 recommends a single connection, its default. |
| `pooling.remote.connections-per-host-max` | `4` | `advanced.connection.pool.remote.size` | Approximated | Same as above. |
| `pooling.local.max-requests-per-connection` | `32768` | `advanced.connection.max-requests-per-connection` | Approximated | Applies to all the connections, local and remote. Limited to `32767`, the maximum accepted by the driver 4, values outside of `1..32768` are rejected. The default of the driver 4 is `1024`. |
| `pooling.heartbeat-interval` | `30s` | `advanced.heartbeat.interval` | Exact | |
| `pooling.local.connections-per-host-min` | `1` | – | Ignored | The pool has a fixed size. |
| `pooling.remote.connections-per-host-min` | `1` | – | Ignored | The pool has a fixed size. |
| `pooling.remote.max-requests-per-connection` | `2000` | – | Ignored | `pooling.local.max-requests-per-connection` applies to all the connections. |
| `pooling.local.new-connection-threshold` | `800` | – | Ignored | The pool has a fixed size. |
| `pooling.remote.new-connection-threshold` | `200` | – | Ignored | The pool has a fixed size. |
| `pooling.pool-timeout` | `5s` | – | Ignored | Requests are not queued by the pool, a request goes to the next node if all the connections are busy. Use `advanced.throttler` to limit the concurrency. |
| `pooling.idle-timeout` | `2min` | – | Ignored | Idle connections are checked with heartbeats. |
| `pooling.max-queue-size` | `256` | – | Ignored | Requests are not queued by the pool. Use `advanced.throttler` with `ConcurrencyLimitingRequestThrottler` and its `max-queue-size` to queue the requests. |
| `query.consistency` | `LOCAL_ONE` | `basic.request.consistency` | Exact | Case insensitive. |
| `query.serial-consistency` | `SERIAL` | `basic.request.serial-consistency` | Exact | Case insensitive. |
| `query.fetch-size` | `5000` | `basic.request.page-size` | Exact | |
| `query.default-idempotence` | `false` | `basic.request.default-idempotence` | Exact | |
| `query.refresh-node-list-interval` | `1s` | `advanced.metadata.topology-event-debouncer.window` | Approximated | Applies to all the topology events. |
| `query.max-pending-refresh-node-list-requests` | `20` | `advanced.metadata.topology-event-debouncer.max-events` | Approximated | Applies to all the topology events. |
| `query.refresh-node-interval` | `1s` | – | Ignored | `query.refresh-node-list-interval` applies to all the topology events. |
| `query.max-pending-refresh-node-requests` | `20` | – | Ignored | `query.max-pending-refresh-node-list-requests` applies to all the topology events. |
| `query.refresh-schema-interval` | `1s` | `advanced.metadata.schema.debouncer.window` | Exact | |
| `query.max-pending-refresh-schema-requests` | `20` | `advanced.metadata.schema.debouncer.max-events` | Exact | |
| `query.metadata` | `true` | `advanced.metadata.schema.enabled`, `advanced.metadata.token-map.enabled` | Approximated | Enables or disables both, as in the driver 3. Token-aware routing requires the token map. |
| `query.re-prepare-on-up` | `true` | `advanced.prepared-statements.reprepare-on-up.enabled` | Exact | |
| `query.prepare-on-all-hosts` | `true` | `advanced.prepared-statements.prepare-on-all-nodes` | Exact | |
| `reconnection.min-delay` | `1s` | `advanced.reconnection-policy.base-delay` | Exact | With `ExponentialReconnectionPolicy`. |
| `reconnection.max-delay` | `10min` | `advanced.reconnection-policy.max-delay` | Exact | The default of the driver 4 is `60s`. |
| `socket.connect-timeout` | `5s` | `advanced.connection.connect-timeout` | Exact | |
| `socket.read-timeout` | `12s` | `basic.request.timeout` | Approximated | The read timeout of the driver 3 applied to a single attempt on a single node, the request timeout of the driver 4 applies to the whole request, including retries and speculative executions. The default of the driver 4 is `2s`. |
| `socket.keep-alive` | not set | `advanced.socket.keep-alive` | Exact | |
| `socket.reuse-address` | not set | `advanced.socket.reuse-address` | Exact | |
| `socket.so-linger` | not set | `advanced.socket.linger-interval` | Exact | In seconds, in both drivers. |
| `socket.tcp-no-delay` | `true` | `advanced.socket.tcp-no-delay` | Exact | |
| `socket.receive-buffer-size` | not set | `advanced.socket.receive-buffer-size` | Exact | |
| `socket.send-buffer-size` | not set | `advanced.socket.send-buffer-size` | Exact | |
| `authentication.username` | not set | `advanced.auth-provider.username` | Exact | With `PlainTextAuthProvider`. If the username or the password is missing, the authentication is skipped, as by the reader of scassandra for the driver 3. |
| `authentication.password` | not set | `advanced.auth-provider.password` | Exact | |
| `load-balancing.local-dc` | see the notes | `basic.load-balancing-policy.local-datacenter` | Exact | A non-empty value uses `DefaultLoadBalancingPolicy` with the given local DC. If the `load-balancing` block is absent or the value is empty, `DcInferringLoadBalancingPolicy` infers the local DC from the contact points, as the driver 3 did; the default policy of the driver 4 requires the local DC with explicit contact points. If the block is present, the default is `localDc`, as in the legacy reader. |
| `load-balancing.allow-remote-dcs-for-local-consistency-level` | `false` | `advanced.load-balancing-policy.dc-failover.allow-for-local-consistency-levels` | Approximated | Ignored by scassandra for the driver 3, but applied now. Has no effect unless `advanced.load-balancing-policy.dc-failover.max-nodes-per-remote-dc` is set, as remote nodes are not used by default. |
| `speculative-execution.delay` | `500ms` | `advanced.speculative-execution-policy.delay` | Exact | With `ConstantSpeculativeExecutionPolicy`, if the `speculative-execution` block is present. |
| `speculative-execution.max-executions` | `2` | `advanced.speculative-execution-policy.max-executions` | Approximated | The driver 3 counted the speculative executions only, the driver 4 counts the initial execution as well, so the value is incremented by one. |
| `log-queries` | `false` | `advanced.request-tracker.classes = [RequestLogger]` | Approximated | Logs successful, slow (over `5s`, as the default of the driver 3) and failed requests. The driver 3 logged at `DEBUG`, the driver 4 logs at `INFO` and `WARN` (`ERROR` for failures), so the logs are more verbose. |
| `metrics` | `false` | `advanced.metrics.session.enabled`, `advanced.metrics.node.enabled` | Approximated | Enables a fixed list of the metrics, close to the metrics of the driver 3: `bytes-sent`, `bytes-received`, `connected-nodes`, `cql-requests`, `cql-client-timeouts` for the session, and `pool.open-connections`, `pool.in-flight`, `bytes-sent`, `bytes-received`, `errors.connection.init`, `errors.request.read-timeouts`, `errors.request.write-timeouts`, `errors.request.unavailables`, `errors.request.others`, `retries.total`, `ignores.total`, `speculative-executions` for the nodes. Use the driver configuration to choose the metrics. |
| `jmx-reporting` | `false` | – | Ignored | The driver 4 does not report the metrics to JMX. Use the metrics registry of the session (`session.unwrap.getMetrics`) with a Dropwizard `JmxReporter`, or the Micrometer integration of the driver. |

## Other differences

* The configuration of the driver 4 has options the legacy configuration had no equivalent for, e.g. the
  [execution profiles](https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/configuration/#execution-profiles),
  set them in the `driver` block.
* The keyspace of a session of the driver 4 is fixed when the session is created, set it with
  `basic.session-keyspace` in the `driver` block.
* `NextHostRetryPolicy` is configured in the `driver` block as well, see its Scaladoc.

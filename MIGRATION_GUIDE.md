# Migrating from `scassandra` (driver 3) to `scassandra4` (driver 4)

`scassandra4` is the Cassandra Java driver 4 based successor of `scassandra`.
It mirrors the `scassandra` API name-for-name, keeps the same HOCON config
schema, and — crucially — **coexists with `scassandra` on the same classpath**,
so you can migrate your codebase module-by-module instead of all at once.

## Step 1: add the new dependency (keep the old one)

```scala
libraryDependencies += "com.evolution" %% "scassandra4" % "<version>"
```

Nothing conflicts: the artifacts have different coordinates
(`com.evolution:scassandra4` vs `com.evolutiongaming:scassandra`), different
packages (`com.evolution.scassandra4` vs `com.evolutiongaming.scassandra`), and
the underlying drivers were designed by DataStax to coexist
(`org.apache.cassandra:java-driver-core` with `com.datastax.oss.driver.*`
packages vs `com.datastax.cassandra:cassandra-driver-core` with
`com.datastax.driver.core.*`).

## Step 2: migrate one module at a time

Rewrite imports — the API names are identical:

```
com.evolutiongaming.scassandra   →  com.evolution.scassandra4
com.datastax.driver.core         →  com.datastax.oss.driver.api.core...  (see table below)
```

The usage pattern is unchanged:

```scala
import com.evolution.scassandra4._

val session = for {
  clusterOf <- Resource.eval(CassandraClusterOf.of[IO])
  cluster   <- clusterOf(config)
  session   <- cluster.connect
} yield session
```

**Config files stay untouched**: `scassandra4` reads the same HOCON schema and
translates it to driver 4 options internally (see `CreateDriverConfigLoader`
scaladoc for the full mapping).

## Step 3: finish

When nothing imports `com.evolutiongaming.scassandra` anymore, drop the
`scassandra` dependency. Note that the build tool will not evict it for you
(different coordinates), so remove it explicitly.

## Type mapping

| scassandra (driver 3) | scassandra4 (driver 4) |
|---|---|
| `com.evolutiongaming.scassandra._` | `com.evolution.scassandra4._` |
| `com.evolutiongaming.nel.Nel` (in `CassandraConfig`) | `cats.data.NonEmptyList` |
| `com.datastax.driver.core.Row` | `com.datastax.oss.driver.api.core.cql.Row` |
| `com.datastax.driver.core.ResultSet` | `com.datastax.oss.driver.api.core.cql.AsyncResultSet` |
| `com.datastax.driver.core.PreparedStatement` | `com.datastax.oss.driver.api.core.cql.PreparedStatement` |
| `com.datastax.driver.core.Statement` | `com.datastax.oss.driver.api.core.cql.Statement[?]` |
| `new SimpleStatement(query)` | `SimpleStatement.newInstance(query)` |
| `ConsistencyLevel.LOCAL_QUORUM` | `DefaultConsistencyLevel.LOCAL_QUORUM` |
| `ProtocolVersion.V4` | `DefaultProtocolVersion.V4` |
| `ProtocolOptions.Compression.LZ4` | `Compression.Lz4` (scassandra4's own type) |
| `com.datastax.driver.core.Duration` | `com.datastax.oss.driver.api.core.data.CqlDuration` |
| `com.datastax.driver.core.LocalDate` | `java.time.LocalDate` (native in driver 4) |
| `getTimestamp` / `java.util.Date` | `java.time.Instant` (native in driver 4) |
| `util.FromGFuture` | `util.FromCompletionStage` |
| `GettableByNameData` / `GettableByIndexData` | `GettableByName` / `GettableByIndex` |
| `SettableData[A]` | `SettableByName[A]` / `SettableByIndex[A]` |

All Encode/Decode/Update typeclasses, `DecodeRow`/`EncodeRow`, `ToCql`,
`TableName`, `syntax._` extension methods (`decode`, `decodeAt`, `encode`,
`encodeSome`, `update`, `updateAt`, `trace`, `toCql`), `executeStream`,
`CassandraHealthCheck`, `CreateKeyspaceIfNotExists` and
`ReplicationStrategyConfig` keep their names and shapes.

## Behavioral changes to watch for

- **`execute` returns `AsyncResultSet`, not `ResultSet`.** Driver 4's async
  API exposes one page at a time. `resultSet.one()` still works for
  single-row reads; for multi-row results use `resultSet.stream[F]` or
  `session.executeStream(...)` (which fetch pages in the background) instead
  of iterating the result set directly.
- **Statements are immutable.** Driver 4 setters (and therefore the `encode` /
  `update` syntax) return a *new* statement. Chained code
  (`bound.encode("a", 1).encode("b", 2)`) is unaffected; code that called a
  setter and discarded the result silently did the right thing on driver 3 and
  does nothing on driver 4 — the compiler won't catch this, review such spots.
- **No `Cluster` in driver 4.** `CassandraCluster` is kept as a facade, but
  `clusterName` and `metadata` moved to `CassandraSession` (driver 4 only
  exposes them on a live session), `metadata` now returns the raw driver
  `Metadata` (no `Metadata[F]` wrapper yet), and `newSession` is gone — use
  `connect`.
- **Local datacenter.** Driver 4's load balancing requires a local datacenter.
  If `load-balancing.local-dc` is not configured, scassandra4 falls back to
  inferring it from the contact points (`DcInferringLoadBalancingPolicy`), so
  driver 3 era configs keep working. Setting it explicitly is still
  recommended.
- **Protocol versions V1/V2 are gone** (driver 4 requires Cassandra 2.1+ /
  protocol V3+); configs with `protocol-version = "v1"|"v2"` fail to parse.
- **Compression needs an extra artifact**: driver 4 does not bundle lz4/snappy;
  add `org.lz4:lz4-java` (or `org.xerial.snappy:snappy-java`) when enabling
  compression.
- **Ignored config options** (no driver 4 counterpart): `jmx-reporting`,
  `metrics`, `pooling.pool-timeout`, `pooling.idle-timeout`,
  `pooling.max-queue-size`, `pooling.*.new-connection-threshold`,
  `pooling.*.connections-per-host-min`,
  `pooling.remote.max-requests-per-connection`,
  `query.refresh-node-interval`, `query.max-pending-refresh-node-requests`.
  They are still accepted (configs parse fine) but have no effect.
- **`NextHostRetryPolicy` is not ported** — driver 4's retry policy SPI is
  class-based configuration. Driver 4's default retry policy plus speculative
  executions cover the common cases; a custom policy can be registered via
  `advanced.retry-policy.class` using the raw driver API if needed.
- **Legacy `apply(config: Config)` constructors** (deprecated since 1.1.5) were
  not carried over; load configs via pureconfig `ConfigReader`.

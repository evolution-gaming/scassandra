# Scassandra
[![Build Status](https://github.com/evolution-gaming/scassandra/workflows/CI/badge.svg)](https://github.com/evolution-gaming/scassandra/actions?query=workflow%3ACI)
[![Coverage Status](https://coveralls.io/repos/github/evolution-gaming/scassandra/badge.svg?branch=master)](https://coveralls.io/github/evolution-gaming/scassandra?branch=master)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/0d7a08d9bb95457f95cded06e8c2177c)](https://app.codacy.com/gh/evolution-gaming/scassandra/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Version](https://img.shields.io/badge/version-click-blue)](https://evolution.jfrog.io/artifactory/api/search/latestVersion?g=com.evolutiongaming&a=scassandra_2.13&repos=public)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellowgreen.svg)](https://opensource.org/licenses/MIT)

### Cassandra client in scala - wrapper over java client

| branch       | version | java driver                                         | notes                                   |
|--------------|---------|-----------------------------------------------------|-----------------------------------------|
| `master`     | 6.x     | `org.apache.cassandra:java-driver-core` 4.x         | active development                      |
| `series/5.x` | 5.x     | `com.datastax.cassandra:cassandra-driver-core` 3.x  | bug fixes and dependency updates only   |

## Example

```scala
import com.evolutiongaming.scassandra._
import com.evolutiongaming.scassandra.syntax._

val config = CassandraConfig.Default
val session = for {
  cluster <- CassandraCluster.of[IO](config, clusterId = 0)
  session <- cluster.connect
} yield session

val name = for {
  resultSet <- session.use { session => session.execute("SELECT name FROM users") }
} yield {
  val row = resultSet.one()
  row.decode[String]("name")
}

name.unsafeRunSync()
```

## Setup

```scala
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.1.2")

libraryDependencies += "com.evolutiongaming" %% "scassandra" % "6.0.0"
```

## Migrating from 5.x

6.x moves to the java driver 4, the wrapper API keeps its shape but the driver types changed:

- imports: `com.datastax.driver.core.*` becomes `com.datastax.oss.driver.api.core.*`, e.g. `Row`, `Statement[?]`,
  `SimpleStatement` and `PreparedStatement` live in `com.datastax.oss.driver.api.core.cql`
- `execute` returns `AsyncResultSet`, a single page; use `resultSet.stream[F]` or `session.executeStream`
  to read all pages instead of iterating the result set
- statements are immutable, setters and `encode`/`update` syntax return a new statement
- `new SimpleStatement(query)` becomes `SimpleStatement.newInstance(query)`
- `ConsistencyLevel.LOCAL_QUORUM` becomes `DefaultConsistencyLevel.LOCAL_QUORUM`,
  `ProtocolVersion.V4` becomes `DefaultProtocolVersion.V4`,
  `ProtocolOptions.Compression.LZ4` becomes `Compression.Lz4`
- `com.datastax.driver.core.Duration` becomes `CqlDuration`, `com.datastax.driver.core.LocalDate` is replaced by
  `java.time.LocalDate`
- `CassandraCluster` holds no connection anymore, `clusterName` and `metadata` moved to `CassandraSession`,
  `newSession`, `init`, `state` and `stateSnapshot` are gone, node state is available via `Metadata.nodes`
- `CassandraClusterOf.addClusterJObserveHook` is replaced by `CassandraClusterOf.of(configure)`, which
  receives the driver `CqlSessionBuilder`
- `FromGFuture`, `NextHostRetryPolicy`, `ToJava`, `ToScala` and `ToCql.Ops` are removed
- config: `pooling` follows the driver 4 pool model (`local-size`, `remote-size`, `max-requests-per-connection`,
  `heartbeat-interval`), `jmx-reporting`, `metrics`, `query.refresh-node-interval`,
  `query.max-pending-refresh-node-requests` and `load-balancing.allow-remote-dcs-for-local-consistency-level`
  are gone, unknown keys are ignored so old config files still load
- when `load-balancing.local-dc` is not set the local datacenter is inferred from the contact points
- compression needs `org.lz4:lz4-java` or `org.xerial.snappy:snappy-java` on the classpath

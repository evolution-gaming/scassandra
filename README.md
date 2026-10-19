# Scassandra
[![Build Status](https://github.com/evolution-gaming/scassandra/workflows/CI/badge.svg)](https://github.com/evolution-gaming/scassandra/actions?query=workflow%3ACI)
[![Coverage Status](https://coveralls.io/repos/github/evolution-gaming/scassandra/badge.svg?branch=master)](https://coveralls.io/github/evolution-gaming/scassandra?branch=master)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/0d7a08d9bb95457f95cded06e8c2177c)](https://app.codacy.com/gh/evolution-gaming/scassandra/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Version](https://img.shields.io/badge/version-click-blue)](https://evolution.jfrog.io/artifactory/api/search/latestVersion?g=com.evolutiongaming&a=scassandra_2.13&repos=public)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellowgreen.svg)](https://opensource.org/licenses/MIT)

### Cassandra client in scala - wrapper over java client

## Example

```scala
import com.evolutiongaming.scassandra._

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
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.0.2")

libraryDependencies += "com.evolutiongaming" %% "scassandra" % "3.2.1"
```

## scassandra-v4

`scassandra-v4` is a thin wrapper over the [Java driver 4](https://github.com/apache/cassandra-java-driver).
It is published separately, under a new organization and package, so both versions could be used in the same
application during the migration.

```scala
libraryDependencies += "com.evolution" %% "scassandra-v4" % "<version>"

// translation of the configuration of scassandra for the Java driver 3, depends on both versions
libraryDependencies += "com.evolution" %% "scassandra-v4-compat" % "<version>"
```

### Example

```scala
import cats.effect.IO
import com.evolution.scassandra._
import com.evolution.scassandra.syntax._
import com.typesafe.config.ConfigFactory

val config = CassandraConfig.fromConfig(ConfigFactory.load(), "my-service.cassandra").fold(
  failures => throw new IllegalArgumentException(failures.prettyPrint()),
  identity,
)

val names = CassandraSession.make[IO](config).use { session =>
  session.executeStream("SELECT name FROM users").map(_.decode[String]("name")).toList
}
```

### Configuration

The configuration is the configuration of the Java driver 4
([reference](https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/configuration/reference/)),
which could be read from any path, so an application could have several sessions with different
configurations:

```hocon
my-service.cassandra {
  basic.contact-points = ["host1:9042", "host2:9042"]
  basic.load-balancing-policy.local-datacenter = "dc1"
}
```

* `CassandraConfig.fromConfig(config)` reads the default path of the driver, `datastax-java-driver`;
* `CassandraConfig.fromConfig(config, "my-service.cassandra")` reads the given path;
* `ConfigReader[CassandraConfig]` reads it with pureconfig.

The missing options get the defaults of the Java driver 4, which differ from the defaults of scassandra for the
Java driver 3, e.g. a 2s request timeout instead of 12s.

The configuration of scassandra for the Java driver 3 could be kept as is, and moved to the configuration of the
driver 4 gradually, under the `driver` key:

```hocon
my-service.cassandra {
  contact-points = ["host1", "host2"]
  query.consistency = "LOCAL_QUORUM"

  driver {
    basic.request.timeout = 3 seconds
  }
}
```

`scassandra-v4-compat` reads and translates it, using the reader of scassandra for the Java driver 3, so the
values are interpreted exactly as before, and the missing keys get the defaults of scassandra for the Java
driver 3, not of the Java driver 4:
* `CassandraConfigCompat.fromLegacyConfig(config, "my-service.cassandra")` reads it;
* `CassandraConfigCompat.legacyConfigReader` reads it with pureconfig;
* `CassandraConfigCompat.fromV3(config, driverOverrides)` translates the case class of scassandra for the Java
  driver 3.

They return a `TranslatedConfig`: the `config` to create a session with, and the `warnings` about the keys that
have no equivalent in the Java driver 4 and are ignored. The warnings are not logged, log them yourself.

Once all the keys are moved to `driver`, read it with `CassandraConfig.fromConfig(config, "my-service.cassandra.driver")`
and drop `scassandra-v4-compat`. Mind that the options missing under `driver` then get the defaults of the Java
driver 4 instead of the defaults of scassandra for the Java driver 3, so set every option which should keep its
previous value, e.g. `basic.request.timeout = 12 seconds`.

See [the translation of the legacy configuration](docs/legacy-config-translation.md) for the details on how
each key is translated, and which keys have no equivalent.

### Migrating from scassandra

Most of the API is kept: `CassandraSession`, `DecodeByName`, `DecodeByIdx`, `DecodeRow`, `EncodeByName`,
`EncodeByIdx`, `EncodeRow`, `UpdateByName`, `UpdateByIdx`, `UpdateRow`, `ToCql`, `TableName`,
`ReplicationStrategyConfig`, `CreateKeyspaceIfNotExists`, `CassandraHealthCheck`, `NextHostRetryPolicy` and the
`syntax` object. Replace `com.evolutiongaming.scassandra` with `com.evolution.scassandra` in the imports, and
mind the following differences:

* **Statements are immutable.** `bind().encode("id", id)` returns a new statement, which should be executed,
  while the original one stays unchanged. Code ignoring the returned statement compiles, but loses the values.
* **There is no cluster.** A session of the driver 4 holds the connections and the metadata of the whole
  cluster, so `CassandraCluster` and `CassandraClusterOf` are replaced with `CassandraSession.make`. Use
  `CassandraSession.make(builder)` to customize the session, e.g. to register listeners, codecs or a custom
  configuration loader, instead of `CassandraClusterOf.addClusterJObserveHook`.
* **The keyspace of a session is fixed**, set it with `basic.session-keyspace` instead of `connect(keyspace)`.
* **Results are paged asynchronously.** `execute` returns the driver `AsyncResultSet`, use `stream` or
  `executeStream` to read all the pages. Do not reuse an `AsyncResultSet`: the rows of its current page can be
  read only once, so a second stream of it silently misses them, while `executeStream` executes the statement
  again on each consumption.
* **The driver types are used directly**, e.g. `ConsistencyLevel`, `Metadata`, `CqlDuration`,
  `java.time.LocalDate`, `CqlIdentifier`. The codecs of the driver could be used with `DecodeByName.fromCodec`,
  `fromClass` and `fromGenericType`, and similar methods of the other type classes.
* **The request timeout** applies to the whole request, including retries and speculative executions.
* **Policies are set in the configuration.** The driver 4 creates them itself, so `NextHostRetryPolicy` cannot
  be set on a statement. Set it in the configuration, with the required `max-retries`:
  ```hocon
  advanced.retry-policy {
    class = com.evolution.scassandra.NextHostRetryPolicy
    max-retries = 3
  }
  ```
  To apply it to some statements only, set it in an execution profile, e.g. `profiles.journal.advanced.retry-policy`,
  and select the profile with `statement.setExecutionProfileName("journal")`. The driver 4 has no
  `LoggingRetryPolicy`.
* **`ReplicationStrategyConfig` fails on an unknown strategy or an invalid value**, where v3 silently used
  `SimpleStrategy` with a replication factor of 1.
* Resources are created with `make` instead of `of`, i.e. `CassandraSession.make`, `CassandraHealthCheck.make`.
* **`CassandraHealthCheck.make` takes the session of the application**, not a `Resource` of a session, as a
  session of the driver 4 is heavyweight, and a separate one would not check the session in use.
* Removed:
  * `init`, `newSession` and `state`/`stateSnapshot` of the session, use `session.unwrap` for the driver features
    not covered by the wrapper, e.g. `getMetadata.getNodes` and `Node.getOpenConnections`;
  * the metadata wrappers, use the driver `Metadata` returned by `session.metadata`;
  * `execute(query, values*)`, use `SimpleStatement.newInstance(query, values*)`;
  * `trace`, use `setTracing` of the statement;
  * `FromGFuture`, the driver 4 uses `CompletionStage`;
  * `Masked`, `CassandraConfig.toString` masks the values of the keys like `password`, `secret`, `token` and
    `credential`;
  * the configuration case classes, e.g. `PoolingConfig` and `QueryConfig`, use the configuration of the driver.

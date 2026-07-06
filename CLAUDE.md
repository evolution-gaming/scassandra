# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Scassandra is a Scala wrapper over the DataStax Java Cassandra driver (currently driver 3.x, `com.datastax.driver.core`), exposing a tagless-final, cats-effect API. Published as `com.evolutiongaming::scassandra`. This checkout (`feature/driver-4-poc` branch) is a proof-of-concept for migrating to Java driver 4.

## Commands

```bash
sbt compile              # compile all modules
sbt test                 # run all tests (tests module needs Docker, see below)
sbt build                # alias for: all compile test
sbt check                # alias; normally versionPolicyCheck + doc (currently stubbed to `show version`)
sbt "scassandra/test"    # unit tests only (no Docker needed)
sbt "tests/test"         # integration tests (requires Docker — testcontainers Cassandra)
```

Run a single test suite:

```bash
sbt "scassandra/testOnly com.evolutiongaming.scassandra.CassandraConfigSpec"
```

Cross-building: Scala 2.13.16 (default) and 3.3.7. Use `sbt "++3.3.7 test"` for a specific version or `sbt +test` for all. CI runs both on Java 11. Always verify changes compile on both Scala versions — the codebase has cross-version source dirs and dependency differences.

## Module structure

- `scassandra/` — the published library.
- `tests/` — integration tests against a real Cassandra via testcontainers (`CassandraSpec`); not published, runs forked and non-parallel.

## Architecture

### Effectful wrapper layer

The core chain mirrors the Java driver's object model, wrapped in `F[_]`:

`CassandraClusterOf[F]` (factory, generates unique cluster ids) → `CassandraCluster[F]` (a `Resource`) → `CassandraSession[F]` (a `Resource`). `CreateClusterJ` translates `CassandraConfig` into the Java `Cluster.builder()` calls. All wrapper traits provide `mapK` for natural transformations.

`util/FromGFuture[F]` is the bridge from Guava `ListenableFuture` (what the Java driver 3 returns) to `F[A]`; it is the implicit capability required alongside `Sync`/`Async` throughout.

### Configuration

`CassandraConfig` is a tree of case classes (`PoolingConfig`, `QueryConfig`, `ReconnectionConfig`, `SocketConfig`, `AuthenticationConfig`, `LoadBalancingConfig`, `SpeculativeExecutionConfig`, `CloudSecureConnectBundleConfig`), each with:
- a pureconfig `ConfigReader` (the `*Implicits` files hold Scala 2/3-portable derivation),
- an `asJava`-style conversion applied to the driver builder.

Config specs load HOCON fixtures from `scassandra/src/test/resources/com/evolutiongaming/scassandra/`. When a cloud secure connect bundle is configured, contact points and port are ignored.

### Encode/decode typeclasses

Data mapping between Scala types and driver rows is done via small typeclasses, with instances and derivation in the files of the same name:
- `EncodeByName`/`DecodeByName` and `EncodeByIdx`/`DecodeByIdx` — single column by name or index,
- `EncodeRow`/`DecodeRow` — whole rows,
- `UpdateByName`/`UpdateByIdx`/`UpdateRow` — for bound/settable statements.

`syntax._` provides the user-facing extension methods (`row.decode[A]("name")`, `statement.encode(...)`, `resultSet.stream[F]`).

### Streaming

`syntax.ResultSetOps.stream` and `StreamingCassandraSession.executeStream` expose result sets as an sstream `Stream[F, Row]`, prefetching the next page in the background (`fetchMoreResults` started concurrently while the current page is folded).

### Cross-version source dirs

`scassandra/src/main/scala-2.12|2.13|3/` contain only `ToJava`/`ToScala` (Java↔Scala collection converters, which differ per Scala version). Scala 3 uses `pureconfig-core` while Scala 2 uses full `pureconfig` — derivation code must work with both (see `util/PureconfigUtils`).

## Binary compatibility

This is a published library with `versionPolicyIntention := Compatibility.BinaryCompatible`. Preserving bincompat within a major version is a hard constraint:
- Config case classes keep extra `private[scassandra] def this(...)` secondary constructors when fields are added.
- New trait methods get no-op default implementations, marked with `// TODO: [X.0.0 release]` comments for cleanup at the next major version (see `CassandraClusterOf.addClusterJObserveHook`).

Follow these patterns when changing any public API.

# Migration plan: DataStax Java driver 3 → 4

## Goals

1. Migrate scassandra to the Cassandra Java driver 4.
2. **Driver 3 and driver 4 must be able to run on the same classpath**, so clients can
   migrate incrementally instead of in one big-bang upgrade.
3. Client code changes should be as small as possible.

## TL;DR

Publish a **new artifact** (`com.evolution:scassandra4`) with a **new base package**
(`com.evolution.scassandra4`) that mirrors the existing scassandra API 1:1 in names
and shape, backed by driver 4. Keep the current
`scassandra` artifact on driver 3 untouched (maintenance only). Clients migrate
module-by-module by swapping imports, while both drivers (and both scassandra
artifacts) sit on the same classpath. Keep the existing `CassandraConfig` HOCON schema
and translate it programmatically to driver 4's config, so client deployments don't
need config rewrites.

## Why coexistence works — and what it forces

Driver 3 and driver 4 are deliberately non-conflicting:

| | Driver 3 | Driver 4 |
|---|---|---|
| Coordinates | `com.datastax.cassandra:cassandra-driver-core` | `org.apache.cassandra:java-driver-core` (4.18+, post-Apache-donation) |
| Packages | `com.datastax.driver.core.*` | `com.datastax.oss.driver.*` (kept after donation) |
| Guava | plain dependency | shaded internally |

Different coordinates, different packages, no split packages → they coexist fine. The
only shared dependency to watch is Netty (both use 4.1.x); if a client hits a
conflict, driver 4 ships a `java-driver-core-shaded` variant.

But this cuts the other way for scassandra itself: the public API **leaks driver types
everywhere** (`ResultSet`, `Row`, `PreparedStatement`, `GettableByNameData`,
`TypeCodec`, `ProtocolVersion`, …). A same-package, same-artifact "upgrade" is
impossible — a `CassandraSession` compiled against driver 3 and one compiled against
driver 4 cannot share an FQCN on one classpath. Therefore:

**The new code must live in a new package and a new artifact.** This is not a
workaround; it is the mechanism that lets a client run both halves of their codebase
during migration.

**Decision: `com.evolution.scassandra4`** — combines Evolution's newer org prefix
(`com.evolution`) with an explicit driver-version marker. The fully disjoint prefix
avoids old/new import mix-ups during migration, and the differing groupId means the
build tool never evicts one artifact in favor of the other (same coexistence
mechanism the drivers themselves use). Post-migration caveat: the old
`com.evolutiongaming:scassandra` artifact is not evicted automatically either —
clients should add an explicit exclusion or lint check once they finish migrating.

## Module layout

```
scassandra/       — as-is, driver 3, maintenance only (bincompat preserved, sbt-version-policy stays green)
scassandra4/      — new published module (com.evolution:scassandra4), driver 4, package com.evolution.scassandra4
tests/            — integration tests for BOTH drivers, in one module
```

Having both in one `tests` module is itself the proof of the main goal: one spec that
opens a driver-3 session and a driver-4 session against the same testcontainer
validates classpath coexistence on every CI run.

## API strategy: mirror, don't abstract

Do **not** build a driver-agnostic facade (own `Row`/`ResultSet` types): it is a much
bigger project, and clients would have to rewrite all their code against the new
abstraction anyway. Instead, mirror the current API shape name-for-name so client
migration is mostly an import swap.

- `CassandraClusterOf[F]` / `CassandraCluster[F]` / `CassandraSession[F]` — same
  names, same `Resource` lifecycle. Driver 4 merged `Cluster` into `CqlSession`, so
  `CassandraCluster` becomes a thin facade holding the configured session builder;
  `connect` / `connect(keyspace)` build the `CqlSession`, `metadata` delegates to
  `session.getMetadata`. Clients keep the
  `clusterOf(config) → cluster.connect → session` pattern unchanged.
- **Typeclasses port almost 1:1**:

  | Driver 3 type | Driver 4 type |
  |---|---|
  | `GettableByNameData` | `GettableByName` |
  | `GettableData` (by index) | `GettableByIndex` |
  | `SettableData[A]` | `SettableByName[A]` |
  | `Duration` | `CqlDuration` |
  | `LocalDate` (driver's own) | `java.time.LocalDate` (native) |

  `EncodeByName` / `DecodeByName` / `EncodeRow` / `DecodeRow` / `UpdateByName` / etc.
  keep their names and instances. Driver 4 uses `java.time` natively, so the
  `Instant`/`LocalDate` conversion shims mostly disappear.
- `syntax._` and `StreamingCassandraSession` re-implemented over the async API:
  `executeAsync` returns `AsyncResultSet` with `currentPage()` / `hasMorePages` /
  `fetchNextPage()` — this maps more cleanly onto the sstream prefetch pattern than
  driver 3's `fetchMoreResults` did.
- `FromGFuture` dies; driver 4 returns `CompletionStage`, so lifting is
  `Async[F].fromCompletableFuture`. Keep a small `FromCompletionStage[F]` capability
  trait only if the testing seam is wanted; otherwise drop it.

Where types must differ, they differ visibly (compile errors, not behavior changes) —
the main one being that driver 4 `Statement`s are immutable builders
(`SimpleStatement.builder(...)`), so client code that mutated statements needs
mechanical fixes.

## Config strategy: keep the schema, translate underneath

Driver 4 abandoned builder-based options for its own typesafe-config tree
(`datastax-java-driver { ... }`). Forcing clients to rewrite deployment configs would
violate the "minimal changes" goal, so:

- Keep the `CassandraConfig` case-class tree and its HOCON schema **as-is** in the
  new module.
- Implement one translation layer: `CassandraConfig → DriverConfigLoader`
  (programmatic builder), mapping pooling / socket / reconnection /
  speculative-execution / compression / auth options to their driver 4 equivalents.
- For options with no driver 4 counterpart, log a warning rather than fail; document
  the mapping table.
- Add an escape hatch — an optional passthrough for a raw driver-4 `Config` — so
  clients can reach new driver 4 features without waiting for scassandra to model
  them.

### Known gotchas

- **Local datacenter**: driver 4's default load-balancing policy *requires* an
  explicit local datacenter and fails at startup without one. To keep existing
  configs working, default to `DcInferringLoadBalancingPolicy` and add an optional
  `localDatacenter` field to `LoadBalancingConfig`.
- **Metrics/JMX**: `jmxReporting` / `metrics` map to driver 4's config-driven metrics
  (Dropwizard/Micrometer optional modules) — needs a deliberate decision rather than
  a silent drop.
- **Netty version alignment** between the two drivers on one classpath (see above).

## Client migration story

1. Add `com.evolution:scassandra4` alongside `com.evolutiongaming:scassandra` (both
   drivers now on the classpath — no conflict).
2. Per module/component: swap imports (`com.evolutiongaming.scassandra.*` →
   `com.evolution.scassandra4.*`), fix the handful of compile errors from changed
   driver types (statement builders, mainly). Config files stay untouched.
3. When nothing imports the old package anymore, drop `scassandra` and driver 3.

Smooth step 2 with a migration guide (type-mapping table + removed-options table) and
optionally a **scalafix rule** that rewrites the imports automatically — cheap to
write given the 1:1 naming.

## Phasing

1. **Skeleton** — ✅ done: `scassandra4` sbt module (driver
   `org.apache.cassandra:java-driver-core:4.19.0`); `CassandraClusterOf` /
   `CassandraCluster` / `CassandraSession` over `CqlSession`; `FromCompletionStage`
   lifting; minimal `CassandraConfig` (full tree comes in phase 2) with the
   `DcInferringLoadBalancingPolicy` fallback; coexistence integration test
   (`tests/.../CoexistenceSpec.scala`) passing — driver 3 + driver 4 sessions
   querying the same testcontainer on one classpath.
2. **Config translation** — ✅ done: full `CassandraConfig` tree ported with the
   driver 3 HOCON schema (fixtures copied verbatim and passing);
   `CreateDriverConfigLoader` translates it to driver 4 options (mapping and
   ignored options documented in its scaladoc, verified by
   `CreateDriverConfigLoaderSpec`); DC-inference fallback when no `local-dc`.
   Notable choices: `Nel` → cats `NonEmptyList`; driver 3 enums replaced by
   driver 4's `DefaultProtocolVersion`/`DefaultConsistencyLevel` (protocol V1/V2 no
   longer parseable — driver 4 requires V3+) and an own `Compression` type;
   `maxExecutions` keeps driver 3 semantics (translation adds 1 for driver 4's
   initial-execution counting); legacy config-tools `apply(config)` constructors
   dropped — pureconfig `ConfigReader` is the only entry point; `jmx-reporting`
   and `metrics` are accepted but not translated yet.
3. **Data layer** — ✅ done: `EncodeByName`/`DecodeByName`/`EncodeByIdx`/
   `DecodeByIdx`/`EncodeRow`/`DecodeRow`/`UpdateByName`/`UpdateByIdx`/`UpdateRow`
   ported 1:1 against driver 4's `GettableByName`/`GettableByIndex`/
   `SettableByName`/`SettableByIndex`; `ToCql`/`TableName`; `syntax._` with the
   same extension methods (`decode`/`encode`/`encodeSome`/`update`/`trace`);
   `StreamingCassandraSession.executeStream` and `AsyncResultSet.stream` over
   sstream with next-page prefetch while the current page is folded. Notable
   changes: `java.time.Instant`/`LocalDate` are native (driver 3's `LocalDate`
   shims gone), `Duration` → `CqlDuration`, and driver 4 settables are
   immutable — the typeclass shape already threads the returned instance, so
   only clients that ignored the return value are affected. Unit specs ported
   (in-memory `DataMock` over the driver 4 interfaces), plus `Cassandra4Spec`
   integration test covering prepared statements, encode syntax, and streaming
   with paging (`fetchSize = 2` over 5 rows).
4. **Polish** — ✅ done: `CassandraHealthCheck` (driver 4 `ConsistencyLevel`,
   same API), `ReplicationStrategyConfig` + `CreateKeyspaceIfNotExists`
   (cats `NonEmptyList`, verbatim fixture passing), client-facing
   [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) with the type-mapping and
   behavioral-changes tables, README section. Not ported (documented in the
   guide): `NextHostRetryPolicy` (driver 4 retry SPI is config-class based),
   the `Metadata[F]` wrapper (raw driver `Metadata` exposed for now). The
   scalafix rule was skipped — the import rewrite is mechanical and covered by
   the guide.

## Versioning / compatibility

- `scassandra4` is a brand-new artifact: no bincompat constraints initially (mark
  it `Compatibility.None` until its API settles).
- The driver 3 `scassandra` artifact stays frozen and green under
  `Compatibility.BinaryCompatible`.
- Deprecate the driver 3 artifact once clients have migrated.

## Decisions

- New coordinates and base package: `com.evolution:scassandra4`, package
  `com.evolution.scassandra4` (decided 2026-07-06).

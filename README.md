# Scassandra
[![Build Status](https://github.com/evolution-gaming/scassandra/workflows/CI/badge.svg)](https://github.com/evolution-gaming/scassandra/actions?query=workflow%3ACI)
[![Coverage Status](https://coveralls.io/repos/github/evolution-gaming/scassandra/badge.svg?branch=master)](https://coveralls.io/github/evolution-gaming/scassandra?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7b5aa36e03bf4e84ad0abd019d3d587b)](https://www.codacy.com/app/evolution-gaming/scassandra?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=evolution-gaming/scassandra&amp;utm_campaign=Badge_Grade)
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

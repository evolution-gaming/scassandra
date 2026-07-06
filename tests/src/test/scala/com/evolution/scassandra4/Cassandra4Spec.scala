package com.evolution.scassandra4

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.syntax.all._
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel
import com.dimafeng.testcontainers.CassandraContainer
import com.evolution.scassandra4.StreamingCassandraSession._
import com.evolution.scassandra4.syntax._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.utility.DockerImageName

/** Exercises the driver 4 based data layer end-to-end: DDL, prepared
  * statements with the encode syntax, and streaming with a page size small
  * enough to force background page fetching.
  */
class Cassandra4Spec extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  private lazy val container = CassandraContainer(
    image = DockerImageName.parse("cassandra:3.11.7"),
  )

  override def beforeAll() = {
    super.beforeAll()
    container.start()
  }

  override def afterAll() = {
    container.stop()
    super.afterAll()
  }

  private def sessionOf(config: CassandraConfig): Resource[IO, CassandraSession[IO]] = {
    for {
      clusterOf <- Resource.eval(CassandraClusterOf.of[IO])
      cluster   <- clusterOf(config)
      session   <- cluster.connect
    } yield session
  }

  private lazy val config = CassandraConfig.Default.copy(
    contactPoints = NonEmptyList.of(s"${ container.containerIpAddress }:${ container.mappedPort(9042) }"),
  )

  "scassandra4" should {

    "insert with prepared statements and stream with paging" in {

      val session = sessionOf(config.copy(query = QueryConfig(fetchSize = 2)))

      val result = session.use { session =>
        for {
          _        <- session.execute(
            "CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1}")
          _        <- session.execute(
            "CREATE TABLE IF NOT EXISTS test.records (id INT PRIMARY KEY, name TEXT)")
          prepared <- session.prepare("INSERT INTO test.records (id, name) VALUES (:id, :name)")
          _        <- (0 until 5).toList.traverse_ { id =>
            val bound = prepared
              .bind()
              .encode("id", id)
              .encode("name", s"name-$id")
            session.execute(bound)
          }
          rows     <- session
            .executeStream("SELECT id, name FROM test.records")
            .toList
        } yield {
          rows should have size 5
          rows
            .map { row => (row.decode[Int]("id"), row.decode[String]("name")) }
            .sorted shouldEqual (0 until 5).toList.map { id => (id, s"name-$id") }
        }
      }

      result.unsafeRunSync()
    }

    "perform the health check statement" in {
      val result = sessionOf(config).use { session =>
        for {
          statement <- CassandraHealthCheck.Statement.of[IO](session, DefaultConsistencyLevel.LOCAL_ONE)
          _         <- statement
        } yield ()
      }
      result.unsafeRunSync()
    }
  }
}

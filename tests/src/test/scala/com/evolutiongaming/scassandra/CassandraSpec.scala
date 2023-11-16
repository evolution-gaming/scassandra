package com.evolutiongaming.scassandra

import cats.arrow.FunctionK
import cats.effect.unsafe.implicits
import cats.effect.{IO, Resource}
import cats.implicits._
import com.datastax.driver.core.{Duration, Row}
import com.dimafeng.testcontainers.CassandraContainer
import org.testcontainers.utility.DockerImageName
import com.evolutiongaming.catshelper.CatsHelper._
import com.evolutiongaming.catshelper.ToTry
import com.evolutiongaming.scassandra.IOSuite._
import com.evolutiongaming.scassandra.syntax._
import org.scalatest.BeforeAndAfterAll
import com.evolutiongaming.sstream.Stream._
import com.evolutiongaming.nel.Nel

import scala.util.Try
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class CassandraSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  private lazy val config = 
    CassandraConfig.Default.copy(
      contactPoints = Nel(cassandraContainer.containerIpAddress),
      port = cassandraContainer.mappedPort(9042),
    )

  private lazy val cassandraContainer = CassandraContainer(
    dockerImageNameOverride = DockerImageName.parse("cassandra:3.11.7"),
  )

  // due to test structure we need to start the container before the test suite
  cassandraContainer.start()

  implicit val toTry: ToTry[IO] = ToTry.ioToTry(implicits.global)

  private lazy val (cluster, clusterRelease) = {
    val cassandraClusterOf = CassandraClusterOf.of[IO]
    val cassandraCluster = for {
      cassandraClusterOf <- Resource.eval(cassandraClusterOf)
      cassandraCluster   <- cassandraClusterOf(config)
    } yield {
      cassandraCluster.mapK(FunctionK.id)
    }

    cassandraCluster.allocated.toTry.get
  }

  private lazy val (session, sessionRelease) = cluster.connect.allocated.toTry.get

  override def afterAll() = {
    cassandraContainer.stop()
    super.afterAll()
  }

  "Cassandra" should {

    "clusterName" in {
      cluster.clusterName.toTry.get should startWith(config.name)
    }

    "connect" in {
      session
    }

    val keyspace = "tmp_keyspace"

    val table = "tmp_table"

    "Session" should {

      "init" in {
        session.init.toTry.get
      }

      "State" should {

        "connectedHosts" in {
          session.state.connectedHosts.toTry.get.nonEmpty shouldEqual true
        }

        "openConnections" in {
          val state = session.state
          for {
            host <- state.connectedHosts.toTry.get
          } {
            state.openConnections(host).toTry.get should be > 0
          }
        }

        "trashedConnections" in {
          val state = session.state
          for {
            host <- state.connectedHosts.toTry.get
          } {
            state.trashedConnections(host).toTry.get shouldEqual 0
          }
        }

        "inFlightQueries" in {
          val state = session.state
          for {
            host <- state.connectedHosts.toTry.get
          } {
            state.inFlightQueries(host).toTry.get shouldEqual 0
          }
        }
      }


      "create keyspace" in {
        val query = CreateKeyspaceIfNotExists(keyspace, ReplicationStrategyConfig.Default)
        session.execute(query).toTry.get
      }

      "create table" in {
        val query = s"CREATE TABLE IF NOT EXISTS $keyspace.$table (key TEXT PRIMARY KEY, value TEXT, duration DURATION)"
        session.execute(query).toTry.get
      }

      val duration = Duration.newInstance(1, 1, 1)

      "insert" in {
        val query = s"INSERT INTO $keyspace.$table (key, value, duration) VALUES (?, ?, ?)"
        val result = for {
          prepared <- session.prepare(query)
          bound     = prepared.bind()
            .encode("key", "key")
            .encode("value", "value")
            .encode("duration", duration)
          result   <- session.execute(bound)
        } yield {
          Option(result.one())
        }

        result.toTry shouldEqual none.pure[Try]
      }

      "select" in {

        def decodeRow(row: Row) = {
          val value = row.decode[String]("value")
          val duration = row.decode[Duration]("duration")
          (value, duration)
        }

        val query = s"SELECT value, duration FROM $keyspace.$table WHERE key = ?"
        val result = for {
          prepared <- session.prepare(query)
          bound     = prepared.bind().encode("key", "key")
          result   <- session.execute(bound)
        } yield for {
          row <- Option(result.one())
        } yield decodeRow(row)

        result.toTry shouldEqual ("value", duration).some.pure[Try]

        val resultStream = for {
          resultSet  <- session.execute(query, Map("key" -> "key"))
          stream = resultSet.stream[IO]
          row <- stream.first
        } yield for {
          row <- row
        } yield decodeRow(row)

        resultStream.toTry shouldEqual ("value", duration).some.pure[Try]
      }
    }


    lazy val metadata = cluster.metadata.toTry.get

    "Metadata" should {

      "clusterName" in {
        metadata.clusterName.toTry.get shouldEqual "Test Cluster"
      }

      "schema" in {
        metadata.schema.toTry.get should startWith("CREATE KEYSPACE system_traces")
      }

      lazy val keyspaceMetadata = cluster.metadata.toTry.get.keyspace(keyspace).toTry.get

      "keyspace" in {
        keyspaceMetadata.isDefined shouldEqual true
      }

      "keyspaces" in {
        cluster.metadata.toTry.get.keyspaces.toTry.get.map(_.name).toSet shouldEqual Set(
          keyspace,
          "system_traces",
          "system",
          "system_distributed",
          "system_schema",
          "system_auth")
      }

      "KeyspaceMetadata" should {

        lazy val keyspaceMetadata1 = keyspaceMetadata.get

        "name" in {
          keyspaceMetadata1.name shouldEqual keyspace
        }

        "schema" in {
          keyspaceMetadata1.schema.toTry.get should startWith("CREATE KEYSPACE tmp_keyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;")
        }

        "asCql" in {
          keyspaceMetadata1.asCql.toTry.get shouldEqual "CREATE KEYSPACE tmp_keyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;"
        }

        "tables" in {
          keyspaceMetadata1.tables.toTry.get.map(_.name).toSet shouldEqual Set(table)
        }

        "table" in {
          keyspaceMetadata1.table(table).toTry.get.map(_.name) shouldEqual Some(table)
        }

        "durableWrites" in {
          keyspaceMetadata1.durableWrites shouldEqual true
        }

        "virtual" in {
          keyspaceMetadata1.virtual shouldEqual false
        }

        "replication" in {
          keyspaceMetadata1.replication.toTry.get shouldEqual Map(
            ("class", "org.apache.cassandra.locator.SimpleStrategy"),
            ("replication_factor", "1"))
        }

        "userTypes" in {
          keyspaceMetadata1.userTypes.toTry.get shouldEqual List.empty
        }
      }
    }

    "session.close" in {
      sessionRelease.toTry.get
    }

    "cluster.close" in {
      clusterRelease.toTry.get
    }
  }
}

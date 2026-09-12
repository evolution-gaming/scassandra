package com.evolutiongaming.scassandra

import cats.arrow.FunctionK
import cats.effect.IO
import cats.implicits.*
import com.datastax.driver.core.{Duration, Row}
import com.evolutiongaming.catshelper.CatsHelper.*
import com.evolutiongaming.scassandra.syntax.*
import com.evolutiongaming.sstream.Stream.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.annotation.nowarn
import scala.util.Try

class CassandraSpec extends AnyWordSpec with CassandraSuite with Matchers {

  override protected def keyspace: String = "tmp_keyspace"

  private lazy val cluster1 = cluster.mapK(FunctionK.id)

  "Cassandra" should {

    "clusterName" in {
      cluster1.clusterName.toTry.get should startWith(config.name)
    }

    "connect" in {
      session
    }

    val table = "tmp_table"

    "Session" should {

      "init" in {
        session.init.toTry.get
      }

      "state" should {

        @nowarn("cat=deprecation")
        def getState(session: CassandraSession[IO] = session): CassandraSession.State[IO] = session.state

        "connectedHosts" in {
          getState().connectedHosts.toTry.get.nonEmpty shouldEqual true
        }

        "openConnections" in {
          for {
            host <- getState().connectedHosts.toTry.get
          } {
            getState().openConnections(host).toTry.get should be > 0
          }
        }

        "trashedConnections" in {
          for {
            host <- getState().connectedHosts.toTry.get
          } {
            getState().trashedConnections(host).toTry.get shouldEqual 0
          }
        }

        "inFlightQueries" in {
          for {
            host <- getState().connectedHosts.toTry.get
          } {
            getState().inFlightQueries(host).toTry.get shouldEqual 0
          }
        }

        // newSession returns an unconnected session, so connections established by
        // init are only visible if state takes a fresh snapshot on each access
        //
        // see CassandraSession.state scaladoc for more info about the bug this test case is
        // checking
        "reflect connections established after construction" in {
          val hosts = cluster.newSession
            .use { newSession =>
              for {
                _ <- newSession.init
                hosts <- getState(newSession).connectedHosts
              } yield hosts
            }
            .toTry
            .get
          hosts.nonEmpty shouldEqual true
        }
      }

      "stateSnapshot" should {

        def getStateSnapshot: CassandraSession.StateSnapshot = session.stateSnapshot.toTry.get

        "connectedHosts" in {
          getStateSnapshot.connectedHosts.nonEmpty shouldEqual true
        }

        "openConnections" in {
          for {
            host <- getStateSnapshot.connectedHosts
          } {
            getStateSnapshot.openConnections(host) should be > 0
          }
        }

        "trashedConnections" in {
          for {
            host <- getStateSnapshot.connectedHosts
          } {
            getStateSnapshot.trashedConnections(host) shouldEqual 0
          }
        }

        "inFlightQueries" in {
          for {
            host <- getStateSnapshot.connectedHosts
          } {
            getStateSnapshot.inFlightQueries(host) shouldEqual 0
          }
        }
      }

      "create keyspace" in {
        val query = CreateKeyspaceIfNotExists(keyspace, ReplicationStrategyConfig.Default)
        session.execute(query).toTry.get
      }

      "create table" in {
        val query =
          s"CREATE TABLE IF NOT EXISTS $keyspace.$table (key TEXT PRIMARY KEY, value TEXT, duration DURATION)"
        session.execute(query).toTry.get
      }

      val duration = Duration.newInstance(1, 1, 1)

      "insert" in {
        val query = s"INSERT INTO $keyspace.$table (key, value, duration) VALUES (?, ?, ?)"
        val result = for {
          prepared <- session.prepare(query)
          bound = prepared.bind()
            .encode("key", "key")
            .encode("value", "value")
            .encode("duration", duration)
          result <- session.execute(bound)
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
          bound = prepared.bind().encode("key", "key")
          result <- session.execute(bound)
        } yield {
          for {
            row <- Option(result.one())
          } yield decodeRow(row)
        }

        result.toTry shouldEqual ("value", duration).some.pure[Try]

        val resultStream = for {
          resultSet <- session.execute(query, Map("key" -> "key"))
          stream = resultSet.stream[IO]
          row <- stream.first
        } yield {
          for {
            row <- row
          } yield decodeRow(row)
        }

        resultStream.toTry shouldEqual ("value", duration).some.pure[Try]
      }

      "insert and select with positional values" in {
        val insert = s"INSERT INTO $keyspace.$table (key, value) VALUES (?, ?)"
        session.execute(insert, "key1", "value1").toTry.get

        val select = s"SELECT value FROM $keyspace.$table WHERE key = ?"
        val result = for {
          result <- session.execute(select, "key1")
        } yield {
          for {
            row <- Option(result.one())
          } yield row.decode[String]("value")
        }

        result.toTry shouldEqual "value1".some.pure[Try]
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
        cluster.metadata.toTry.get.keyspaces.toTry.get.map(_.name).toSet should contain allOf (
          keyspace,
          "system_traces",
          "system",
          "system_distributed",
          "system_schema",
          "system_auth",
        )
      }

      "KeyspaceMetadata" should {

        lazy val keyspaceMetadata1 = keyspaceMetadata.get

        "name" in {
          keyspaceMetadata1.name shouldEqual keyspace
        }

        "schema" in {
          keyspaceMetadata1.schema.toTry.get should startWith(
            "CREATE KEYSPACE tmp_keyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;",
          )
        }

        "asCql" in {
          keyspaceMetadata1.asCql.toTry.get shouldEqual
            "CREATE KEYSPACE tmp_keyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;"
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
            ("replication_factor", "1"),
          )
        }

        "userTypes" in {
          keyspaceMetadata1.userTypes.toTry.get shouldEqual List.empty
        }
      }
    }

  }
}

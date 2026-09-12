package com.evolutiongaming.scassandra

import cats.arrow.FunctionK
import cats.effect.IO
import cats.implicits.*
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.data.CqlDuration
import com.evolutiongaming.catshelper.CatsHelper.*
import com.evolutiongaming.scassandra.syntax.*
import com.evolutiongaming.sstream.Stream.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Try

class CassandraSpec extends AnyWordSpec with CassandraSuite with Matchers {

  override protected def keyspace: String = "tmp_keyspace"

  private lazy val cluster1 = cluster.mapK(FunctionK.id)

  "Cassandra" should {

    "connect" in {
      session
    }

    "connect via mapK" in {
      cluster1.connect.use(_.loggedKeyspace).toTry.get shouldEqual None
    }

    val table = "tmp_table"

    "Session" should {

      "create keyspace" in {
        val query = CreateKeyspaceIfNotExists(keyspace, ReplicationStrategyConfig.Default)
        session.execute(query).toTry.get
      }

      "create table" in {
        val query =
          s"CREATE TABLE IF NOT EXISTS $keyspace.$table (key TEXT PRIMARY KEY, value TEXT, duration DURATION)"
        session.execute(query).toTry.get
      }

      val duration = CqlDuration.newInstance(1, 1, 1)

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
          val duration = row.decode[CqlDuration]("duration")
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

        val rows = for {
          prepared <- session.prepare(query)
          bound = prepared.bind().encode("key", "key")
          result <- session.execute(bound)
          rows <- result.stream[IO].toList
        } yield rows.map(decodeRow)

        rows.toTry shouldEqual List(("value", duration)).pure[Try]
      }

      "execute with positional values" in {
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

      "execute with named values" in {
        val insert = s"INSERT INTO $keyspace.$table (key, value) VALUES (:key, :value)"
        session.execute(insert, Map[String, AnyRef]("key" -> "key2", "value" -> "value2")).toTry.get

        val select = s"SELECT value FROM $keyspace.$table WHERE key = :key"
        val result = for {
          result <- session.execute(select, Map[String, AnyRef]("key" -> "key2"))
        } yield {
          for {
            row <- Option(result.one())
          } yield row.decode[String]("value")
        }

        result.toTry shouldEqual "value2".some.pure[Try]
      }
    }

    lazy val metadata = session.metadata.toTry.get

    "Metadata" should {

      "clusterName" in {
        metadata.clusterName.toTry.get shouldEqual Some("Test Cluster")
      }

      "nodes" in {
        metadata.nodes.toTry.get.map(_.getOpenConnections) should not be empty
      }

      lazy val keyspaceMetadata = session.metadata.toTry.get.keyspace(keyspace).toTry.get

      "keyspace" in {
        keyspaceMetadata.isDefined shouldEqual true
      }

      "keyspaces" in {
        session.metadata.toTry.get.keyspaces.toTry.get.map(_.name).toSet should contain allOf (
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
          val schema = keyspaceMetadata1.schema.toTry.get
          schema should include(s"CREATE KEYSPACE $keyspace")
          schema should include(s"CREATE TABLE $keyspace.$table")
        }

        "asCql" in {
          val cql = keyspaceMetadata1.asCql.toTry.get
          cql should include(s"CREATE KEYSPACE $keyspace")
          cql should include("SimpleStrategy")
          cql should not include "CREATE TABLE"
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

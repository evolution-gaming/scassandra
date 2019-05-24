package com.evolutiongaming.scassandra

import com.evolutiongaming.cassandra.StartCassandra
import com.evolutiongaming.concurrent.CurrentThreadExecutionContext
import com.evolutiongaming.scassandra.syntax._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class CassandraSpec extends WordSpec with BeforeAndAfterAll with Matchers {

  private val config = CassandraConfig.Default

  implicit val ec = CurrentThreadExecutionContext

  private lazy val shutdownCassandra = StartCassandra()

  private val cluster = CreateCluster(config)

  private val timeout = 30.seconds

  private lazy val session = Await.result(cluster.connect(), timeout)

  override def beforeAll() = {
    super.beforeAll()
    shutdownCassandra
    ()
  }

  override def afterAll() = {
    Await.result(cluster.close(), timeout)
    shutdownCassandra()
    super.afterAll()
  }

  "Cassandra" should {

    "clusterName" in {
      cluster.clusterName should startWith(config.name)
    }

    "isClosed" in {
      cluster.isClosed shouldEqual false
    }

    "connect" in {
      session
    }

    val keyspace = "tmp_keyspace"

    val table = "tmp_table"

    "Session" should {

      "init" in {
        Await.result(session.init, timeout)
      }

      "closed" in {
        session.closed shouldEqual false
      }

      "State" should {

        "connectedHosts" in {
          session.state.connectedHosts.nonEmpty shouldEqual true
        }

        "openConnections" in {
          val state = session.state
          for {
            host <- state.connectedHosts
          } {
            state.openConnections(host) should be > 0
          }
        }

        "trashedConnections" in {
          val state = session.state
          for {
            host <- state.connectedHosts
          } {
            state.trashedConnections(host) shouldEqual 0
          }
        }

        "inFlightQueries" in {
          val state = session.state
          for {
            host <- state.connectedHosts
          } {
            state.inFlightQueries(host) shouldEqual 0
          }
        }
      }


      "create keyspace" in {
        val query = CreateKeyspaceIfNotExists(keyspace, ReplicationStrategyConfig.Default)
        Await.result(session.execute(query), timeout)
      }

      "create table" in {
        val query = s"CREATE TABLE IF NOT EXISTS $keyspace.$table (key TEXT PRIMARY KEY, value TEXT, timestamp TIMESTAMP)"
        Await.result(session.execute(query), timeout)
      }

      "select" in {
        val query = s"SELECT value FROM $keyspace.$table WHERE key = ?"
        val result = for {
          prepared <- session.prepare(query)
          bound = prepared
            .bind()
            .encode("key", "key")
          result <- session.execute(bound)
        } yield {
          Option(result.one())
        }

        Await.result(result, timeout) shouldEqual None
      }
    }


    lazy val metadata = cluster.metadata

    "Metadata" should {

      "clusterName" in {
        metadata.clusterName shouldEqual "Test Cluster"
      }

      "schema" in {
        metadata.schema() should startWith("CREATE KEYSPACE system_traces")
      }

      lazy val keyspaceMetadata = cluster.metadata.keyspace(keyspace)

      "keyspace" in {
        keyspaceMetadata.isDefined shouldEqual true
      }

      "keyspaces" in {
        cluster.metadata.keyspaces().map(_.name).toSet shouldEqual Set(
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
          keyspaceMetadata1.schema() should startWith("CREATE KEYSPACE tmp_keyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;")
        }

        "asCql" in {
          keyspaceMetadata1.asCql() shouldEqual "CREATE KEYSPACE tmp_keyspace WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = true;"
        }

        "tables" in {
          keyspaceMetadata1.tables().map(_.name).toSet shouldEqual Set(table)
        }

        "table" in {
          keyspaceMetadata1.table(table).map(_.name) shouldEqual Some(table)
        }

        "durableWrites" in {
          keyspaceMetadata1.durableWrites shouldEqual true
        }

        "virtual" in {
          keyspaceMetadata1.virtual shouldEqual false
        }

        "replication" in {
          keyspaceMetadata1.replication shouldEqual Map(
            ("class", "org.apache.cassandra.locator.SimpleStrategy"),
            ("replication_factor", "1"))
        }
      }
    }

    "session.close" in {
      Await.result(session.close(), timeout)
    }
  }
}

package com.evolutiongaming.scassandra

import com.evolutiongaming.cassandra.StartCassandra
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class CassandraSpec extends WordSpec with BeforeAndAfterAll with Matchers {

  private val config = CassandraConfig.Default

  private lazy val shutdownCassandra = StartCassandra()

  private val cluster = CreateCluster(config)

  private val timeout = 30.seconds

  private lazy val session = Await.result(cluster.connect(), timeout)

  override def beforeAll() = {
    super.beforeAll()
    shutdownCassandra
  }

  override def afterAll() = {
    Await.result(cluster.close(), timeout)
    shutdownCassandra()
    super.afterAll()
  }

  "Cassandra" should {

    "clusterName" in {
      cluster.clusterName.startsWith(config.name) shouldEqual true
    }

    "connect" in {
      session
    }

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
    }
  }
}

package com.evolution.scassandra4

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.syntax.all._
import com.dimafeng.testcontainers.CassandraContainer
import com.evolutiongaming.nel.Nel
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.utility.DockerImageName

import scala.concurrent.duration._

import com.evolutiongaming.{scassandra => v3}
import com.evolution.{scassandra4 => v4}

/** Proves the main goal of MIGRATION_PLAN.md: the driver 3 based `scassandra`
  * and the driver 4 based `scassandra4` run on the same classpath, against the
  * same Cassandra cluster, at the same time.
  */
class CoexistenceSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers {

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

  "driver 3 and driver 4 clients" should {

    "connect and query on the same classpath at the same time" in {

      val host = container.containerIpAddress
      val port = container.mappedPort(9042)

      val v3Session: Resource[IO, v3.CassandraSession[IO]] = {
        val config = v3.CassandraConfig.Default.copy(
          contactPoints = Nel(host),
          port = port,
        )
        for {
          clusterOf <- Resource.eval(v3.CassandraClusterOf.of[IO])
          cluster   <- clusterOf(config)
          session   <- cluster.connect
        } yield session
      }

      val v4Session: Resource[IO, v4.CassandraSession[IO]] = {
        // non-default values to exercise the driver 3 era schema → driver 4 translation
        val config = v4.CassandraConfig.Default.copy(
          contactPoints = NonEmptyList.of(s"$host:$port"),
          query = v4.QueryConfig(fetchSize = 100),
          socket = v4.SocketConfig(connectTimeout = 10.seconds, readTimeout = 20.seconds),
          speculativeExecution = Some(v4.SpeculativeExecutionConfig()),
        )
        for {
          clusterOf <- Resource.eval(v4.CassandraClusterOf.of[IO])
          cluster   <- clusterOf(config)
          session   <- cluster.connect
        } yield session
      }

      val query = "SELECT release_version FROM system.local"

      val result = (v3Session, v4Session).tupled.use { case (v3Session, v4Session) =>
        for {
          v3ResultSet <- v3Session.execute(query)
          v4ResultSet <- v4Session.execute(query)
          v3Version   <- IO { v3ResultSet.one().getString("release_version") }
          v4Version   <- IO { v4ResultSet.one().getString("release_version") }
          v4Cluster   <- v4Session.clusterName
        } yield {
          v3Version shouldEqual v4Version
          v4Cluster shouldEqual "Test Cluster".some
        }
      }

      result.unsafeRunSync()
    }
  }
}

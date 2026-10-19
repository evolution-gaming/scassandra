package com.evolution.scassandra.compat

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.CassandraContainer
import com.evolution.scassandra.CassandraSession
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.CassandraCluster
import com.evolutiongaming.scassandra.CassandraConfig as CassandraConfigV3
import com.evolutiongaming.scassandra.util.FromGFuture
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import scala.jdk.CollectionConverters.*

class TwoDcSessionSpec extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private implicit val fromGFutureIO: FromGFuture[IO] = FromGFuture.lift1[IO]

  private val network = Network.newNetwork()

  private def node(dc: String, token: Long): CassandraContainer = {
    val image = sys.env.getOrElse("CASSANDRA_IMAGE", "cassandra:3.11.7")
    val container = CassandraContainer(DockerImageName.parse(image))
    container.container
      .withNetwork(network)
      .withNetworkAliases(s"$dc-node")
      .withEnv("CASSANDRA_CLUSTER_NAME", "two-dc")
      .withEnv("CASSANDRA_DC", dc)
      .withEnv("CASSANDRA_SEEDS", "dc1-node")
      .withEnv("JVM_OPTS", s"-Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=$token")
      .withStartupTimeout(Duration.ofMinutes(5))
    container.start()
    container
  }

  private lazy val nodes: List[CassandraContainer] = List(node("dc1", 0L), node("dc2", 1L))

  private def ip(container: CassandraContainer): String =
    container.container.getContainerInfo.getNetworkSettings.getNetworks.asScala.values.head.getIpAddress

  private lazy val configV3 =
    CassandraConfigV3.Default.copy(contactPoints = Nel(ip(nodes.head), ip(nodes(1))))

  override protected def afterAll(): Unit = {
    nodes.foreach(_.stop())
    network.close()
    super.afterAll()
  }

  test("driver 3 connects when the contact points are in different DCs and no local DC is set") {
    val now = CassandraCluster.of[IO](configV3, clusterId = 0).use { cluster =>
      cluster.connect.use { session =>
        session.execute("SELECT now() FROM system.local").map { resultSet => Option(resultSet.one()) }
      }
    }

    now.unsafeRunSync() should not be empty
  }

  test("translated driver 3 config connects when the contact points are in different DCs and no local DC is set") {
    val translated =
      CassandraConfigCompat.fromV3(configV3).fold(failures => fail(failures.prettyPrint()), identity)
    val now = CassandraSession.make[IO](translated.config).use { session =>
      session.execute("SELECT now() FROM system.local").map { resultSet => Option(resultSet.one()) }
    }

    now.unsafeRunSync() should not be empty
  }
}

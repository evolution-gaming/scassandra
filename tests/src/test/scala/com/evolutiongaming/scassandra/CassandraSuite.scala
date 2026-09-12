package com.evolutiongaming.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits
import com.dimafeng.testcontainers.CassandraContainer
import com.evolutiongaming.catshelper.CatsHelper.*
import com.evolutiongaming.catshelper.ToTry
import com.evolutiongaming.nel.Nel
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.utility.DockerImageName

import java.time.Duration

object CassandraSuite {

  val image: String = sys.env.getOrElse("CASSANDRA_IMAGE", "cassandra:3.11.7")

  lazy val container: CassandraContainer = {
    val container = CassandraContainer(DockerImageName.parse(image))
    container.container.withStartupTimeout(Duration.ofMinutes(3))
    container.start()
    container
  }

  lazy val config: CassandraConfig = CassandraConfig.Default.copy(
    contactPoints = Nel(container.containerIpAddress),
    port = container.mappedPort(9042),
  )
}

trait CassandraSuite extends BeforeAndAfterAll { self: Suite =>

  implicit val toTry: ToTry[IO] = ToTry.ioToTry(implicits.global)

  protected def keyspace: String = self.getClass.getSimpleName.toLowerCase.filter(_.isLetter)

  protected def config: CassandraConfig = CassandraSuite.config

  private lazy val allocated = {
    val resource = for {
      clusterOf <- CassandraClusterOf.of[IO].toResource
      cluster <- clusterOf(config)
      session <- cluster.connect
    } yield (cluster, session)
    resource.allocated.toTry.get
  }

  protected lazy val cluster: CassandraCluster[IO] = allocated._1._1

  protected lazy val session: CassandraSession[IO] = allocated._1._2

  protected def createKeyspace: IO[Unit] = {
    session.execute(CreateKeyspaceIfNotExists(keyspace, ReplicationStrategyConfig.Default)).void
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createKeyspace.toTry.get
  }

  override protected def afterAll(): Unit = {
    allocated._2.toTry.get
    super.afterAll()
  }
}

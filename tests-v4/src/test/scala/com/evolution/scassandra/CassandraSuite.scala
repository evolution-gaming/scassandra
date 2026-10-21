package com.evolution.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.CassandraContainer
import com.typesafe.config.{Config, ConfigFactory}
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

  // `localhost` is resolved to an IPv6 address as well, which is not exposed by the container
  lazy val host: String = container.containerIpAddress match {
    case "localhost" => "127.0.0.1"
    case host => host
  }

  lazy val port: Int = container.mappedPort(9042)

  lazy val contactPoint: String = s"$host:$port"

  lazy val driverConfig: Config = ConfigFactory.parseString(
    s"""
       |basic.contact-points = ["$contactPoint"]
       |basic.load-balancing-policy.local-datacenter = datacenter1
       |""".stripMargin,
  )

  lazy val config: CassandraConfig = CassandraConfig(driverConfig)
}

trait CassandraSuite extends BeforeAndAfterAll { self: Suite =>

  protected def keyspace: String = self.getClass.getSimpleName.toLowerCase.filter(_.isLetter)

  protected def config: CassandraConfig = CassandraSuite.config

  private lazy val (allocatedSession, releaseSession) =
    CassandraSession.make[IO](config).allocated.unsafeRunSync()

  protected lazy val session: CassandraSession[IO] = allocatedSession

  /**
   * Creates a session with the given driver configuration on top of the default one
   */
  protected def withSession[A](driverConfig: String)(use: CassandraSession[IO] => IO[A]): A = {
    val config1 = CassandraConfig(ConfigFactory.parseString(driverConfig).withFallback(config.driver))
    CassandraSession.make[IO](config1).use(use).unsafeRunSync()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    session.execute(CreateKeyspaceIfNotExists(
      keyspace,
      ReplicationStrategyConfig.Default,
    )).void.unsafeRunSync()
  }

  override protected def afterAll(): Unit = {
    releaseSession.unsafeRunSync()
    super.afterAll()
  }
}

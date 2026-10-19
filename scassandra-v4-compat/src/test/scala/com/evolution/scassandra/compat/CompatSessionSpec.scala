package com.evolution.scassandra.compat

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.internal.core.connection.ConstantReconnectionPolicy
import com.dimafeng.testcontainers.CassandraContainer
import com.evolution.scassandra.CassandraSession
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.CassandraConfig as CassandraConfigV3
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import scala.jdk.OptionConverters.*

/**
 * Checks that the driver 4 works with the dependencies pinned for the driver 3, i.e.
 * Netty
 */
class CompatSessionSpec extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private lazy val container: CassandraContainer = {
    val image = sys.env.getOrElse("CASSANDRA_IMAGE", "cassandra:3.11.7")
    val container = CassandraContainer(DockerImageName.parse(image))
    container.container.withStartupTimeout(Duration.ofMinutes(3))
    container.start()
    container
  }

  override protected def afterAll(): Unit = {
    container.stop()
    super.afterAll()
  }

  test("session created from the translated driver 3 configuration executes queries") {
    val configV3 = CassandraConfigV3.Default.copy(
      contactPoints = Nel(container.containerIpAddress),
      port = container.mappedPort(9042),
    )
    val translated =
      CassandraConfigCompat.fromV3(configV3).fold(failures => fail(failures.prettyPrint()), identity)
    val now = CassandraSession.make[IO](translated.config).use { session =>
      session.execute("SELECT now() FROM system.local").map { resultSet => Option(resultSet.one()) }
    }

    now.unsafeRunSync() should not be empty
  }

  test("session created from the legacy configuration with the driver configuration on top") {
    val hocon = ConfigFactory.parseString(
      s"""
         |cassandra {
         |  contact-points = ["${ container.containerIpAddress }"]
         |  port = ${ container.mappedPort(9042) }
         |  query.fetch-size = 4
         |  log-queries = true
         |  metrics = true
         |  speculative-execution {}
         |  pooling.max-queue-size = 10
         |  driver {
         |    basic.request.timeout = 5s
         |  }
         |}
         |""".stripMargin,
    )
    val translated = CassandraConfigCompat.fromLegacyConfig(hocon, "cassandra").fold(
      failures => fail(failures.prettyPrint()),
      identity,
    )
    val program = CassandraSession.make[IO](translated.config).use { session =>
      for {
        resultSet <- session.execute("SELECT now() FROM system.local")
      } yield {
        val profile = session.unwrap.getContext.getConfig.getDefaultProfile
        val metrics = session.unwrap.getMetrics.toScala.map(_.getRegistry.getNames.size())
        (
          Option(resultSet.one()).isDefined,
          profile.getInt(DefaultDriverOption.REQUEST_PAGE_SIZE),
          profile.getDuration(DefaultDriverOption.REQUEST_TIMEOUT),
          metrics.exists(_ > 0),
        )
      }
    }

    translated.warnings.size shouldEqual 1
    program.unsafeRunSync() shouldEqual ((true, 4, Duration.ofSeconds(5), true))
  }

  test("session uses the reconnection policy set in the driver configuration") {
    val hocon = ConfigFactory.parseString(
      s"""
         |cassandra {
         |  contact-points = ["${ container.containerIpAddress }"]
         |  port = ${ container.mappedPort(9042) }
         |  driver.advanced.reconnection-policy {
         |    class = ConstantReconnectionPolicy
         |    base-delay = 5s
         |  }
         |}
         |""".stripMargin,
    )
    val translated = CassandraConfigCompat.fromLegacyConfig(hocon, "cassandra").fold(
      failures => fail(failures.prettyPrint()),
      identity,
    )
    val program = CassandraSession.make[IO](translated.config).use { session =>
      IO {
        val policy = session.unwrap.getContext.getReconnectionPolicy
        val node = session.unwrap.getMetadata.getNodes.values().iterator().next()
        (policy, policy.newNodeSchedule(node).nextDelay())
      }
    }

    val (policy, delay) = program.unsafeRunSync()
    policy shouldBe a[ConstantReconnectionPolicy]
    delay shouldEqual Duration.ofSeconds(5)
  }
}

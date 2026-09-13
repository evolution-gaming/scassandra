package com.evolutiongaming.scassandra

import cats.arrow.FunctionK
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref, Resource}
import com.datastax.oss.driver.api.core.{AllNodesFailedException, CqlSessionBuilder}
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.MockSupport.notSupported
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.net.InetSocketAddress
import scala.concurrent.duration.*

class CassandraClusterSpec extends AnyWordSpec with Matchers {

  private val unreachable = CassandraConfig(
    contactPoints = Nel("127.0.0.1:1"),
    socket = SocketConfig(connectTimeout = 1.second, readTimeout = 1.second),
    authentication = Some(AuthenticationConfig("user", "pass")),
    loadBalancing = Some(LoadBalancingConfig(localDc = "dc1")),
    speculativeExecution = Some(SpeculativeExecutionConfig()),
    logQueries = true,
  )

  "CreateCqlSessionBuilder" should {

    "accept contact points with and without port" in {
      val config = CassandraConfig(port = 9043, contactPoints = Nel("127.0.0.1:9044", " 127.0.0.2 "))
      CreateCqlSessionBuilder.contactPoints(config) shouldEqual List(
        new InetSocketAddress("127.0.0.1", 9044),
        new InetSocketAddress("127.0.0.2", 9043),
      )
    }

    "accept bracketed ipv6 contact points" in {
      val config = CassandraConfig(port = 9043, contactPoints = Nel("[2001:db8::1]:9044", " [::1] "))
      CreateCqlSessionBuilder.contactPoints(config) shouldEqual List(
        new InetSocketAddress("2001:db8::1", 9044),
        new InetSocketAddress("::1", 9043),
      )
    }

    "reject malformed contact points" in {
      for (contactPoint <- List("a:b:c", "127.0.0.1:port", "node:", "[::1]:", "[::1", "")) {
        val error = the[IllegalArgumentException] thrownBy
          CreateCqlSessionBuilder(CassandraConfig(contactPoints = Nel(contactPoint)), "name")
        error.getMessage should include(contactPoint)
      }
    }

    "build a session builder for a cloud secure connect bundle" in {
      val file =
        CassandraConfig(cloudSecureConnectBundle = Some(CloudSecureConnectBundleConfig.File("/bundle")))
      val url =
        CassandraConfig(cloudSecureConnectBundle = Some(CloudSecureConnectBundleConfig.Url("http://ws")))
      CreateCqlSessionBuilder(file, "name") should not be null
      CreateCqlSessionBuilder(url, "name") should not be null
    }
  }

  "CassandraCluster" should {

    "fail to connect when no node is reachable" in {
      val program = CassandraCluster.of[IO](unreachable, clusterId = 1).use(_.connect.use_)
      a[AllNodesFailedException] should be thrownBy program.unsafeRunSync()
    }

    "apply the builder hook on connect" in {
      val program = for {
        hooks <- Ref[IO].of(0)
        cluster = CassandraCluster.of[IO](
          unreachable,
          clusterId = 1,
          { (builder: CqlSessionBuilder) =>
            hooks.update(_ + 1).unsafeRunSync()
            builder
          },
        )
        _ <- cluster.use(_.connect("ks").use_).attempt
        hooks <- hooks.get
      } yield hooks
      program.unsafeRunSync() shouldEqual 1
    }

    "mapK" in {
      val cluster = new CassandraCluster[IO] {
        def connect: Resource[IO, CassandraSession[IO]] = Resource.pure(new CassandraSessionMock)
        def connect(keyspace: String): Resource[IO, CassandraSession[IO]] = notSupported
      }
      cluster.mapK(FunctionK.id[IO]).connect.use(_ => IO.unit).unsafeRunSync()
    }
  }

  "CassandraClusterOf" should {

    "apply the builder hook on connect" in {
      val program = for {
        hooks <- Ref[IO].of(0)
        clusterOf <- CassandraClusterOf.of[IO] { (builder: CqlSessionBuilder) =>
          hooks.update(_ + 1).unsafeRunSync()
          builder
        }
        _ <- clusterOf(unreachable).use(_.connect.use_).attempt
        hooks <- hooks.get
      } yield hooks
      program.unsafeRunSync() shouldEqual 1
    }

    "create clusters without a hook" in {
      val program = for {
        clusterOf <- CassandraClusterOf.of[IO]
        _ <- clusterOf(unreachable).use(_ => IO.unit)
      } yield ()
      program.unsafeRunSync()
    }
  }
}

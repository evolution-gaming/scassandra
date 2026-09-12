package com.evolutiongaming.scassandra

import cats.arrow.FunctionK
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref, Resource}
import com.datastax.driver.core.ProtocolOptions.Compression
import com.datastax.driver.core.policies.{
  ConstantSpeculativeExecutionPolicy,
  DCAwareRoundRobinPolicy,
  ExponentialReconnectionPolicy,
  NoSpeculativeExecutionPolicy,
  TokenAwarePolicy,
}
import com.datastax.driver.core.{AuthProvider, Cluster as ClusterJ, PlainTextAuthProvider}
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.MockSupport.notSupported
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.*

class CassandraClusterSpec extends AnyWordSpec with Matchers {

  private def withClusterJ[A](config: CassandraConfig, clusterId: Int = 1)(f: ClusterJ => A): A = {
    val cluster = CreateClusterJ(config, clusterId)
    try f(cluster)
    finally cluster.close()
  }

  "CreateClusterJ" should {

    "append the cluster id to the cluster name" in {
      withClusterJ(CassandraConfig(name = "name"), clusterId = 7) { _.getClusterName shouldEqual "name-7" }
    }

    "apply socket, query, pooling and reconnection config" in {
      val config = CassandraConfig(
        port = 9043,
        socket = SocketConfig(readTimeout = 1.second),
        query = QueryConfig(fetchSize = 42),
        pooling = PoolingConfig(maxQueueSize = 7),
        reconnection = ReconnectionConfig(minDelay = 2.seconds, maxDelay = 3.seconds),
        compression = Compression.NONE,
      )
      withClusterJ(config) { cluster =>
        val configuration = cluster.getConfiguration
        configuration.getSocketOptions.getReadTimeoutMillis shouldEqual 1000
        configuration.getQueryOptions.getFetchSize shouldEqual 42
        configuration.getPoolingOptions.getMaxQueueSize shouldEqual 7
        configuration.getProtocolOptions.getPort shouldEqual 9043
        configuration.getProtocolOptions.getCompression shouldEqual Compression.NONE
        val reconnection = configuration.getPolicies.getReconnectionPolicy
        reconnection shouldBe a[ExponentialReconnectionPolicy]
        reconnection.asInstanceOf[ExponentialReconnectionPolicy].getBaseDelayMs shouldEqual 2000
        reconnection.asInstanceOf[ExponentialReconnectionPolicy].getMaxDelayMs shouldEqual 3000
      }
    }

    "apply load balancing policy when set" in {
      withClusterJ(CassandraConfig(loadBalancing = Some(LoadBalancingConfig(localDc = "dc1")))) { cluster =>
        val policy = cluster.getConfiguration.getPolicies.getLoadBalancingPolicy
        policy shouldBe a[TokenAwarePolicy]
        policy.asInstanceOf[TokenAwarePolicy].getChildPolicy shouldBe a[DCAwareRoundRobinPolicy]
      }
    }

    "apply speculative execution policy when set" in {
      withClusterJ(CassandraConfig(speculativeExecution = Some(SpeculativeExecutionConfig()))) {
        _.getConfiguration.getPolicies.getSpeculativeExecutionPolicy shouldBe
          a[ConstantSpeculativeExecutionPolicy]
      }
      withClusterJ(CassandraConfig()) {
        _.getConfiguration.getPolicies.getSpeculativeExecutionPolicy shouldBe a[NoSpeculativeExecutionPolicy]
      }
    }

    "apply credentials when set" in {
      withClusterJ(CassandraConfig(authentication = Some(AuthenticationConfig("user", Masked("pass"))))) {
        _.getConfiguration.getProtocolOptions.getAuthProvider shouldBe a[PlainTextAuthProvider]
      }
      withClusterJ(CassandraConfig()) {
        _.getConfiguration.getProtocolOptions.getAuthProvider shouldBe theSameInstanceAs(AuthProvider.NONE)
      }
    }

    "disable metrics and JMX reporting by default" in {
      withClusterJ(CassandraConfig()) { cluster =>
        cluster.getConfiguration.getMetricsOptions.isEnabled shouldEqual false
        cluster.getConfiguration.getMetricsOptions.isJMXReportingEnabled shouldEqual false
      }
      withClusterJ(CassandraConfig(metrics = true, jmxReporting = true)) { cluster =>
        cluster.getConfiguration.getMetricsOptions.isEnabled shouldEqual true
        cluster.getConfiguration.getMetricsOptions.isJMXReportingEnabled shouldEqual true
      }
    }

    "accept contact points with and without port" in {
      withClusterJ(CassandraConfig(contactPoints = Nel("127.0.0.1:9043", " 127.0.0.2 "))) { _ => () }
    }

    "reject malformed contact points" in {
      val error = the[IllegalArgumentException] thrownBy
        CreateClusterJ(CassandraConfig(contactPoints = Nel("a:b:c")), 1)
      error.getMessage should include("a:b:c")
      a[NumberFormatException] should be thrownBy
        CreateClusterJ(CassandraConfig(contactPoints = Nel("127.0.0.1:port")), 1)
    }
  }

  "CassandraClusterOf" should {

    "assign incrementing cluster ids" in {
      val program = for {
        clusterOf <- CassandraClusterOf.of[IO]
        name1 <- clusterOf(CassandraConfig()).use(_.clusterName)
        name2 <- clusterOf(CassandraConfig()).use(_.clusterName)
      } yield (name1, name2)
      program.unsafeRunSync() shouldEqual (("cluster-1", "cluster-2"))
    }

    "run observe hooks in order of registration" in {
      val program = for {
        ref <- Ref[IO].of(List.empty[String])
        clusterOf <- CassandraClusterOf.of[IO]
        clusterOf1 = clusterOf
          .addClusterJObserveHook(cluster => ref.update(_ :+ s"a:${ cluster.getClusterName }"))
          .addClusterJObserveHook(cluster => ref.update(_ :+ s"b:${ cluster.getClusterName }"))
        _ <- clusterOf1(CassandraConfig()).use(_ => IO.unit)
        hooks <- ref.get
      } yield hooks
      program.unsafeRunSync() shouldEqual List("a:cluster-1", "b:cluster-1")
    }

    "remove all observe hooks" in {
      val program = for {
        ref <- Ref[IO].of(List.empty[String])
        clusterOf <- CassandraClusterOf.of[IO]
        clusterOf1 = clusterOf
          .addClusterJObserveHook(cluster => ref.update(_ :+ cluster.getClusterName))
          .removeAllClusterJObserveHooks()
        _ <- clusterOf1(CassandraConfig()).use(_ => IO.unit)
        hooks <- ref.get
      } yield hooks
      program.unsafeRunSync() shouldEqual Nil
    }
  }

  "CassandraCluster" should {

    "expose the cluster name" in {
      CassandraCluster.of[IO](
        CassandraConfig(name = "name"),
        clusterId = 3,
      ).use(_.clusterName).unsafeRunSync() shouldEqual "name-3"
    }

    "mapK" in {
      val cluster = new CassandraCluster[IO] {
        def connect: Resource[IO, CassandraSession[IO]] = notSupported
        def connect(keyspace: String): Resource[IO, CassandraSession[IO]] = notSupported
        def clusterName: IO[String] = IO.pure("name")
        def newSession: Resource[IO, CassandraSession[IO]] = notSupported
        def metadata: IO[Metadata[IO]] = notSupported
      }
      cluster.mapK(FunctionK.id[IO]).clusterName.unsafeRunSync() shouldEqual "name"
    }
  }
}

package com.evolution.scassandra

import cats.effect.{MonadCancel, Resource, Async}
import cats.implicits._
import cats.~>
import com.datastax.driver.core.{Cluster => ClusterJ}
import com.evolutiongaming.scassandra.util.FromGFuture
import com.evolutiongaming.scassandra.Metadata
import com.evolutiongaming.scassandra.CassandraConfig
import com.evolutiongaming.scassandra.CreateClusterJ

trait CassandraCluster[F[_]] {

  def connect: Resource[F, CassandraSession[F]]

  def connect(keyspace: String): Resource[F, CassandraSession[F]]

  def clusterName: F[String]

  def newSession: Resource[F, CassandraSession[F]]

  def metadata: F[Metadata[F]]
}

object CassandraCluster {

  def apply[F[_]](implicit F: CassandraCluster[F]): CassandraCluster[F] = F


  def apply[F[_] : Async](cluster: ClusterJ): CassandraCluster[F] = {

    new CassandraCluster[F] {

      val connect = {
        CassandraSession.of {
          FromGFuture[F].apply { cluster.connectAsync() }
        }
      }

      def connect(keyspace: String) = {
        CassandraSession.of {
          FromGFuture[F].apply { cluster.connectAsync(keyspace) }
        }
      }

      val clusterName = {
        Async[F].delay { cluster.getClusterName }
      }

      val newSession = {
        CassandraSession.of {
          Async[F].delay { cluster.newSession() }
        }
      }

      val metadata = {
        for {
          metadata <- Async[F].delay { cluster.getMetadata }
        } yield {
          Metadata(metadata)
        }
      }
    }
  }


  def of[F[_]: Async](
    config: CassandraConfig,
    clusterId: Int
  ): Resource[F, CassandraCluster[F]] = {
    val clusterJ = Async[F].delay { CreateClusterJ(config, clusterId) }
    of(clusterJ)
  }


  def of[F[_]: Async](cluster: F[ClusterJ]): Resource[F, CassandraCluster[F]] = {
    val result = for {
      cluster <- cluster
    } yield {
      val release = FromGFuture[F].apply { cluster.closeAsync() }.void
      (CassandraCluster(cluster), release)
    }
    Resource(result)
  }


  implicit class CassandraClusterOps[F[_]](val self: CassandraCluster[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G, g: G ~> F)(implicit F: MonadCancel[F, _], G: MonadCancel[G, _]): CassandraCluster[G] = new CassandraCluster[G] {

      def connect = {
        for {
          a <- self.connect.mapK(f)
        } yield {
          a.mapK(f, g)
        }
      }

      def connect(keyspace: String) = {
        for {
          a <- self.connect(keyspace).mapK(f)
        } yield {
          a.mapK(f, g)
        }
      }

      def clusterName = f(self.clusterName)

      def newSession = {
        for {
          a <- self.newSession.mapK(f)
        } yield {
          a.mapK(f, g)
        }
      }

      def metadata = {
        for {
          a <- f(self.metadata)
        } yield {
          a.mapK(f)
        }
      }
    }
  }
}

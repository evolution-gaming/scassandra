package com.evolutiongaming.scassandra

import cats.effect.{Resource, Sync}
import cats.implicits._
import cats.~>
import com.datastax.driver.core.{Cluster => ClusterJ}
import com.evolutiongaming.scassandra.util.FromGFuture

trait CassandraCluster[F[_]] {

  def connect: Resource[F, Session[F]]

  def connect(keyspace: String): Resource[F, Session[F]]

  def clusterName: F[String]

  def newSession: Resource[F, Session[F]]

  def metadata: F[Metadata]
}

object CassandraCluster {

  def apply[F[_] : Sync : FromGFuture](cluster: ClusterJ): CassandraCluster[F] = {

    new CassandraCluster[F] {

      val connect = {
        Session.of {
          FromGFuture[F].apply { cluster.connectAsync() }
        }
      }

      def connect(keyspace: String) = {
        Session.of {
          FromGFuture[F].apply { cluster.connectAsync(keyspace) }
        }
      }

      val clusterName = {
        Sync[F].delay { cluster.getClusterName }
      }

      val newSession = {
        Session.of {
          Sync[F].delay { cluster.newSession() }
        }
      }

      val metadata = {
        for {
          metadata <- Sync[F].delay { cluster.getMetadata }
        } yield {
          Metadata(metadata)
        }
      }
    }
  }


  def of[F[_] : Sync : FromGFuture](
    config: CassandraConfig,
    clusterId: Int
  ): Resource[F, CassandraCluster[F]] = {
    val clusterJ = Sync[F].delay { CreateClusterJ(config, clusterId) }
    of(clusterJ)
  }


  def of[F[_] : Sync : FromGFuture](cluster: F[ClusterJ]): Resource[F, CassandraCluster[F]] = {
    val result = for {
      cluster <- cluster
    } yield {
      val release = FromGFuture[F].apply { cluster.closeAsync() }.void
      (CassandraCluster(cluster), release)
    }
    Resource(result)
  }


  implicit class ClusterOps[F[_]](val self: CassandraCluster[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G)(implicit F: Sync[F], G: Sync[G]): CassandraCluster[G] = new CassandraCluster[G] {

      def connect = {
        for {
          a <- self.connect.mapK(f)
        } yield {
          a.mapK(f)
        }
      }

      def connect(keyspace: String) = {
        for {
          a <- self.connect(keyspace).mapK(f)
        } yield {
          a.mapK(f)
        }
      }

      def clusterName = f(self.clusterName)

      def newSession = {
        for {
          a <- self.newSession.mapK(f)
        } yield {
          a.mapK(f)
        }
      }

      def metadata = f(self.metadata)
    }
  }
}
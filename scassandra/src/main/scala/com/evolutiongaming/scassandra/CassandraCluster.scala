package com.evolutiongaming.scassandra

import cats.effect.{MonadCancel, Resource, Sync}
import cats.implicits.*
import cats.~>
import com.datastax.driver.core.Cluster as ClusterJ
import com.evolutiongaming.scassandra.util.FromGFuture

trait CassandraCluster[F[_]] {

  def connect: Resource[F, CassandraSession[F]]

  def connect(keyspace: String): Resource[F, CassandraSession[F]]

  def clusterName: F[String]

  def newSession: Resource[F, CassandraSession[F]]

  def metadata: F[Metadata[F]]
}

object CassandraCluster {

  def apply[F[_]](
    implicit
    F: CassandraCluster[F],
  ): CassandraCluster[F] = F

  def apply[F[_]: Sync: FromGFuture](cluster: ClusterJ): CassandraCluster[F] = {

    new CassandraCluster[F] {

      override val connect: Resource[F, CassandraSession[F]] = CassandraSession.of {
        FromGFuture[F].apply { cluster.connectAsync() }
      }

      override def connect(keyspace: String): Resource[F, CassandraSession[F]] = CassandraSession.of {
        FromGFuture[F].apply { cluster.connectAsync(keyspace) }
      }

      override val clusterName: F[String] = Sync[F].delay { cluster.getClusterName }

      override val newSession: Resource[F, CassandraSession[F]] = CassandraSession.of {
        Sync[F].delay { cluster.newSession() }
      }

      override val metadata: F[Metadata[F]] = Sync[F].delay { Metadata(cluster.getMetadata) }
    }
  }

  def of[F[_]: Sync: FromGFuture](
    config: CassandraConfig,
    clusterId: Int,
  ): Resource[F, CassandraCluster[F]] = {
    val clusterJ = Sync[F].delay { CreateClusterJ(config, clusterId) }
    of(clusterJ)
  }

  def of[F[_]: Sync: FromGFuture](cluster: F[ClusterJ]): Resource[F, CassandraCluster[F]] = {
    val result = for {
      cluster <- cluster
    } yield {
      val release = FromGFuture[F].apply { cluster.closeAsync() }.void
      (CassandraCluster(cluster), release)
    }
    Resource(result)
  }

  implicit class CassandraClusterOps[F[_]](val self: CassandraCluster[F]) extends AnyVal {

    def mapK[G[_]](
      f: F ~> G,
    )(implicit
      F: MonadCancel[F, ?],
      G: MonadCancel[G, ?],
    ): CassandraCluster[G] = new CassandraCluster[G] {

      override def connect: Resource[G, CassandraSession[G]] = {
        for {
          a <- self.connect.mapK(f)
        } yield {
          a.mapK(f)
        }
      }

      override def connect(keyspace: String): Resource[G, CassandraSession[G]] = {
        for {
          a <- self.connect(keyspace).mapK(f)
        } yield {
          a.mapK(f)
        }
      }

      override def clusterName: G[String] = f(self.clusterName)

      override def newSession: Resource[G, CassandraSession[G]] = {
        for {
          a <- self.newSession.mapK(f)
        } yield {
          a.mapK(f)
        }
      }

      override def metadata: G[Metadata[G]] = {
        for {
          a <- f(self.metadata)
        } yield {
          a.mapK(f)
        }
      }
    }
  }
}

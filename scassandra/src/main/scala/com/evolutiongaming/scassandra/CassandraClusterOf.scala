package com.evolutiongaming.scassandra

import cats.effect.{Ref, Resource, Sync}
import cats.implicits._
import com.evolutiongaming.scassandra.util.FromGFuture

trait CassandraClusterOf[F[_]] {

  def apply(config: CassandraConfig): Resource[F, CassandraCluster[F]]
}

object CassandraClusterOf {

  def apply[F[_]](implicit F: CassandraClusterOf[F]): CassandraClusterOf[F] = F
  

  def of[F[_] : Sync : FromGFuture]: F[CassandraClusterOf[F]] = {
    for {
      clusterId  <- Ref[F].of(0)
      clusterId1  = clusterId.modify { a =>
        val a1 = a + 1
        (a1, a1)
      }
    } yield {
      apply(clusterId1)
    }
  }


  private def apply[F[_] : Sync : FromGFuture](clusterId: F[Int]): CassandraClusterOf[F] = {
    new CassandraClusterOf[F] {
      def apply(config: CassandraConfig) = {
        val cluster = for {
          clusterId <- clusterId
          cluster   <- Sync[F].delay { CreateClusterJ(config, clusterId) }
        } yield cluster
        CassandraCluster.of(cluster)
      }
    }
  }
}
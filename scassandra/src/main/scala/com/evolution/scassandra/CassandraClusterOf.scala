package com.evolution.scassandra

import cats.effect.{Ref, Resource, Async}
import cats.implicits._
import com.evolutiongaming.scassandra.CassandraConfig
import com.evolutiongaming.scassandra.CreateClusterJ

trait CassandraClusterOf[F[_]] {

  def apply(config: CassandraConfig): Resource[F, CassandraCluster[F]]
}

object CassandraClusterOf {

  def apply[F[_]](implicit F: CassandraClusterOf[F]): CassandraClusterOf[F] = F
  

  def of[F[_]: Async]: F[CassandraClusterOf[F]] = {
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


  private def apply[F[_]: Async](clusterId: F[Int]): CassandraClusterOf[F] = {
    new CassandraClusterOf[F] {
      def apply(config: CassandraConfig) = {
        val cluster = for {
          clusterId <- clusterId
          cluster   <- Async[F].delay { CreateClusterJ(config, clusterId) }
        } yield cluster
        CassandraCluster.of(cluster)
      }
    }
  }
}

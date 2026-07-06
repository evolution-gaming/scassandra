package com.evolution.scassandra4

import cats.effect.{Ref, Resource, Sync}
import cats.syntax.all._
import com.evolution.scassandra4.util.FromCompletionStage

trait CassandraClusterOf[F[_]] {

  def apply(config: CassandraConfig): Resource[F, CassandraCluster[F]]
}

object CassandraClusterOf {

  def apply[F[_]](implicit F: CassandraClusterOf[F]): CassandraClusterOf[F] = F


  def of[F[_]: Sync: FromCompletionStage]: F[CassandraClusterOf[F]] = {
    for {
      clusterIdRef <- Ref[F].of(0)
    } yield {
      val genClusterId = clusterIdRef.updateAndGet(_ + 1)
      new CassandraClusterOf[F] {
        def apply(config: CassandraConfig) = {
          Resource.eval {
            for {
              clusterId <- genClusterId
            } yield {
              CassandraCluster.of[F](config, clusterId)
            }
          }
        }
      }
    }
  }
}

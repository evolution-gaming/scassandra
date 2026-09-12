package com.evolutiongaming.scassandra

import cats.effect.{Async, Ref, Resource}
import cats.implicits.*
import com.datastax.oss.driver.api.core.CqlSessionBuilder

trait CassandraClusterOf[F[_]] {

  def apply(config: CassandraConfig): Resource[F, CassandraCluster[F]]
}

object CassandraClusterOf {

  def apply[F[_]](
    implicit
    F: CassandraClusterOf[F],
  ): CassandraClusterOf[F] = F

  def of[F[_]: Async]: F[CassandraClusterOf[F]] = of { (builder: CqlSessionBuilder) => builder }

  /**
   * @param configure
   *   applied to the session builder of every connect, i.e. to register listeners or to
   *   override driver options
   */
  def of[F[_]: Async](configure: CqlSessionBuilder => CqlSessionBuilder): F[CassandraClusterOf[F]] = {
    for {
      clusterId <- Ref[F].of(0)
    } yield {
      new CassandraClusterOf[F] {
        override def apply(config: CassandraConfig): Resource[F, CassandraCluster[F]] = {
          for {
            clusterId <- Resource.eval(clusterId.updateAndGet(_ + 1))
            cluster <- CassandraCluster.of(config, clusterId, configure)
          } yield cluster
        }
      }
    }
  }
}

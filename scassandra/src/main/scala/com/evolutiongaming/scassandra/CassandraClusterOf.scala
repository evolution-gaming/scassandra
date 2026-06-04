package com.evolutiongaming.scassandra

import cats.Applicative
import cats.effect.{Ref, Resource, Sync}
import cats.implicits.*
import com.datastax.driver.core.Cluster as ClusterJ
import com.evolutiongaming.scassandra.util.FromGFuture

trait CassandraClusterOf[F[_]] {

  def apply(config: CassandraConfig): Resource[F, CassandraCluster[F]]

  /**
   * Creates a new [[CassandraClusterOf]] instance with an added hook for observing the value of
   * the underlying Java Cassandra driver `Cluster` object used for [[CassandraCluster]] creation.
   *
   * This method can be used to register listeners to the Java Cassandra driver `Cluster`.
   */
  def addClusterJObserveHook(f: ClusterJ => F[Unit]): CassandraClusterOf[F] = {
    // TODO: [6.0.0 release] remove this default implementation
    // no-op default implementation to preserve bincompat for 5.5.x

    this
  }

  /**
   * Create a new [[CassandraClusterOf]] instance with all the hooks added by
   * [[addClusterJObserveHook]] method removed.
   */
  def removeAllClusterJObserveHooks(): CassandraClusterOf[F] = {
    // TODO: [6.0.0 release] remove this default implementation
    // no-op default implementation to preserve bincompat for 5.5.x

    this
  }
}

object CassandraClusterOf {

  def apply[F[_]](implicit F: CassandraClusterOf[F]): CassandraClusterOf[F] = F


  def of[F[_] : Sync : FromGFuture]: F[CassandraClusterOf[F]] = {
    for {
      clusterIdRef <- Ref[F].of(0)
    } yield {
      makeImpl(genClusterId = clusterIdRef.updateAndGet(_ + 1))
    }
  }


  private def makeImpl[F[_] : Sync : FromGFuture](genClusterId: F[Int]): CassandraClusterOf[F] = {
    new CassandraClusterOfImpl(
      genClusterId = genClusterId,
      clusterJObserveHook = _ => Applicative[F].unit,
    )
  }

  private final class CassandraClusterOfImpl[F[_] : Sync : FromGFuture]
  (
    genClusterId: F[Int],
    clusterJObserveHook: ClusterJ => F[Unit],
  ) extends CassandraClusterOf[F] {
    override def apply(config: CassandraConfig): Resource[F, CassandraCluster[F]] = {
      val clusterJ = for {
        clusterId <- genClusterId
        clusterJ <- Sync[F].delay {
          CreateClusterJ(config, clusterId)
        }
      } yield clusterJ
      CassandraCluster.of(clusterJ.flatTap(clusterJObserveHook))
    }

    override def addClusterJObserveHook(f: ClusterJ => F[Unit]): CassandraClusterOf[F] = {
      new CassandraClusterOfImpl[F](
        genClusterId = genClusterId,
        clusterJObserveHook = clusterJ => clusterJObserveHook(clusterJ).flatTap(_ => f(clusterJ)),
      )
    }

    override def removeAllClusterJObserveHooks(): CassandraClusterOf[F] = {
      new CassandraClusterOfImpl[F](
        genClusterId = genClusterId,
        clusterJObserveHook = _ => Applicative[F].unit,
      )
    }
  }
}
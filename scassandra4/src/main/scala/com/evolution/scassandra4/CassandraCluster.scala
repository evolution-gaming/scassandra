package com.evolution.scassandra4

import cats.effect.{MonadCancel, Resource, Sync}
import cats.syntax.all._
import cats.~>
import com.evolution.scassandra4.util.FromCompletionStage

/** Mirrors `com.evolutiongaming.scassandra.CassandraCluster`.
  *
  * The Java driver 4 has no `Cluster` anymore — `CqlSession` combines both
  * concepts — so this is a facade over a configured session factory, preserving
  * the `clusterOf(config) → cluster.connect → session` usage pattern of the
  * driver 3 based API.
  */
trait CassandraCluster[F[_]] {

  def connect: Resource[F, CassandraSession[F]]

  def connect(keyspace: String): Resource[F, CassandraSession[F]]
}

object CassandraCluster {

  def apply[F[_]](implicit F: CassandraCluster[F]): CassandraCluster[F] = F


  def of[F[_]: Sync: FromCompletionStage](
    config: CassandraConfig,
    clusterId: Int,
  ): CassandraCluster[F] = {

    def sessionOf(keyspace: Option[String]): Resource[F, CassandraSession[F]] = {
      val acquire = FromCompletionStage[F].apply {
        val builder = CreateCqlSessionBuilder(config, clusterId)
        keyspace
          .fold(builder) { keyspace => builder.withKeyspace(keyspace) }
          .buildAsync()
      }
      Resource
        .make(acquire) { session =>
          FromCompletionStage[F].apply { session.closeAsync() }.void
        }
        .map { session => CassandraSession[F](session) }
    }

    new CassandraCluster[F] {

      val connect = sessionOf(none)

      def connect(keyspace: String) = sessionOf(keyspace.some)
    }
  }


  implicit class CassandraClusterOps[F[_]](val self: CassandraCluster[F]) extends AnyVal {

    def mapK[G[_]](
      f: F ~> G,
    )(implicit
      F: MonadCancel[F, Throwable],
      G: MonadCancel[G, Throwable],
    ): CassandraCluster[G] = {
      new CassandraCluster[G] {

        val connect = self.connect.mapK(f).map(_.mapK(f))

        def connect(keyspace: String) = self.connect(keyspace).mapK(f).map(_.mapK(f))
      }
    }
  }
}

package com.evolutiongaming.scassandra

import cats.effect.{Async, MonadCancel, Ref, Resource, Sync}
import cats.implicits.*
import cats.~>
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.evolutiongaming.scassandra.util.FromCompletionStage

/**
 * Session factory for a Cassandra cluster.
 *
 * The driver has no cluster object anymore, nothing is connected until [[connect]] is
 * called and every call opens a separate session with its own connection pools, named
 * `<config.name>-<clusterId>-<n>` where `n` counts the sessions of this cluster.
 */
trait CassandraCluster[F[_]] {

  def connect: Resource[F, CassandraSession[F]]

  def connect(keyspace: String): Resource[F, CassandraSession[F]]
}

object CassandraCluster {

  def apply[F[_]](
    implicit
    F: CassandraCluster[F],
  ): CassandraCluster[F] = F

  def of[F[_]: Async](
    config: CassandraConfig,
    clusterId: Int,
  ): Resource[F, CassandraCluster[F]] = {
    of(config, clusterId, identity)
  }

  /**
   * @param configure
   *   applied to the session builder of every [[CassandraCluster#connect]] call, i.e. to
   *   register listeners or to override driver options
   */
  def of[F[_]: Async](
    config: CassandraConfig,
    clusterId: Int,
    configure: CqlSessionBuilder => CqlSessionBuilder,
  ): Resource[F, CassandraCluster[F]] = {
    for {
      sessionId <- Resource.eval(Ref[F].of(0))
    } yield {
      val builder = for {
        sessionId <- sessionId.updateAndGet(_ + 1)
        builder <-
          Sync[F].delay { CreateCqlSessionBuilder(config, s"${ config.name }-$clusterId-$sessionId") }
      } yield configure(builder)
      new Impl(builder)
    }
  }

  private final class Impl[F[_]: Async](builder: F[CqlSessionBuilder]) extends CassandraCluster[F] {

    private def session(keyspace: Option[String]): Resource[F, CassandraSession[F]] = {
      CassandraSession.of {
        for {
          builder <- builder
          builder <- Sync[F].delay { keyspace.fold(builder) { keyspace => builder.withKeyspace(keyspace) } }
          session <- FromCompletionStage { builder.buildAsync() }
        } yield session
      }
    }

    override val connect: Resource[F, CassandraSession[F]] = session(None)

    override def connect(keyspace: String): Resource[F, CassandraSession[F]] = session(Some(keyspace))
  }

  implicit class CassandraClusterOps[F[_]](val self: CassandraCluster[F]) extends AnyVal {

    def mapK[G[_]](
      f: F ~> G,
    )(implicit
      F: MonadCancel[F, ?],
      G: MonadCancel[G, ?],
    ): CassandraCluster[G] = new CassandraCluster[G] {

      override def connect: Resource[G, CassandraSession[G]] = self.connect.mapK(f).map(_.mapK(f))

      override def connect(keyspace: String): Resource[G, CassandraSession[G]] =
        self.connect(keyspace).mapK(f).map(_.mapK(f))
    }
  }
}

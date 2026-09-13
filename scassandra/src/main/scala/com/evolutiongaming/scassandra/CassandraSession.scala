package com.evolutiongaming.scassandra

import cats.Functor
import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import cats.~>
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, PreparedStatement, SimpleStatement, Statement}
import com.evolutiongaming.scassandra.util.FromCompletionStage

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/**
 * See [[com.datastax.oss.driver.api.core.CqlSession]]
 */
trait CassandraSession[F[_]] {

  def loggedKeyspace: F[Option[String]]

  /**
   * Returns the first page only, use `stream` from [[syntax]] or
   * [[StreamingCassandraSession]] to read all the pages.
   */
  def execute(query: String): F[AsyncResultSet]

  def execute(query: String, values: Any*): F[AsyncResultSet]

  def execute(query: String, values: Map[String, AnyRef]): F[AsyncResultSet]

  def execute(statement: Statement[?]): F[AsyncResultSet]

  def prepare(query: String): F[PreparedStatement]

  def prepare(statement: SimpleStatement): F[PreparedStatement]

  /**
   * Snapshot of the cluster metadata as known by the session at the moment of the call.
   */
  def metadata: F[Metadata[F]]
}

object CassandraSession {

  def apply[F[_]](
    implicit
    F: CassandraSession[F],
  ): CassandraSession[F] = F

  def apply[F[_]: Async](session: CqlSession): CassandraSession[F] = {

    new CassandraSession[F] {

      override val loggedKeyspace: F[Option[String]] = Sync[F].delay {
        session.getKeyspace.toScala.map(_.asInternal)
      }

      override def execute(query: String): F[AsyncResultSet] = {
        FromCompletionStage { session.executeAsync(query) }
      }

      override def execute(query: String, values: Any*): F[AsyncResultSet] = {
        FromCompletionStage { session.executeAsync(query, values*) }
      }

      override def execute(query: String, values: Map[String, AnyRef]): F[AsyncResultSet] = {
        FromCompletionStage { session.executeAsync(query, values.asJava) }
      }

      override def execute(statement: Statement[?]): F[AsyncResultSet] = {
        FromCompletionStage { session.executeAsync(statement) }
      }

      override def prepare(query: String): F[PreparedStatement] = {
        FromCompletionStage { session.prepareAsync(query) }
      }

      override def prepare(statement: SimpleStatement): F[PreparedStatement] = {
        FromCompletionStage { session.prepareAsync(statement) }
      }

      override val metadata: F[Metadata[F]] = Sync[F].delay { Metadata[F](session.getMetadata) }
    }
  }

  def of[F[_]: Async](session: F[CqlSession]): Resource[F, CassandraSession[F]] = {
    Resource
      .make(session) { session => FromCompletionStage { session.closeAsync() }.void }
      .map { session => CassandraSession[F](session) }
  }

  implicit class SessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def mapK[G[_]: Functor](f: F ~> G): CassandraSession[G] = new CassandraSession[G] {

      override def loggedKeyspace: G[Option[String]] = f(self.loggedKeyspace)

      override def execute(query: String): G[AsyncResultSet] = f(self.execute(query))

      override def execute(query: String, values: Any*): G[AsyncResultSet] = f(self.execute(query, values*))

      override def execute(query: String, values: Map[String, AnyRef]): G[AsyncResultSet] =
        f(self.execute(query, values))

      override def execute(statement: Statement[?]): G[AsyncResultSet] = f(self.execute(statement))

      override def prepare(query: String): G[PreparedStatement] = f(self.prepare(query))

      override def prepare(statement: SimpleStatement): G[PreparedStatement] = f(self.prepare(statement))

      override def metadata: G[Metadata[G]] = f(self.metadata).map(_.mapK(f))
    }
  }
}

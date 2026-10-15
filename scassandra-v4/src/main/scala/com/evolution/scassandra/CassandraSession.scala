package com.evolution.scassandra

import cats.effect.{Async, Resource}
import cats.syntax.all.*
import cats.~>
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, PreparedStatement, SimpleStatement, Statement}
import com.datastax.oss.driver.api.core.metadata.Metadata
import com.datastax.oss.driver.api.core.{CqlIdentifier, CqlSession, CqlSessionBuilder}
import com.evolution.scassandra.util.FromCompletionStage
import com.evolutiongaming.catshelper.LogOf

import scala.jdk.OptionConverters.*

/**
 * Thin effectful wrapper over [[CqlSession]].
 *
 * Unlike the Java driver 3, the session of the Java driver 4 holds the connections and
 * the metadata of the whole cluster, and its keyspace is fixed when the session is
 * created (see `basic.session-keyspace`).
 *
 * Statements of the driver 4 are immutable: setters return a new instance, which should
 * be passed to `execute`.
 */
trait CassandraSession[F[_]] {

  /**
   * The underlying driver session, for the features not covered by this wrapper
   */
  def unwrap: CqlSession

  /**
   * The keyspace the session is connected to, set by `basic.session-keyspace`
   */
  def keyspace: Option[CqlIdentifier]

  def execute(query: String): F[AsyncResultSet]

  def execute(statement: Statement[?]): F[AsyncResultSet]

  def prepare(query: String): F[PreparedStatement]

  def prepare(statement: SimpleStatement): F[PreparedStatement]

  /**
   * Immutable snapshot of the cluster metadata
   */
  def metadata: F[Metadata]
}

object CassandraSession {

  def apply[F[_]](
    implicit
    F: CassandraSession[F],
  ): CassandraSession[F] = F

  /**
   * Wraps an existing session, the session is not closed by the wrapper.
   */
  def apply[F[_]: Async](session: CqlSession): CassandraSession[F] = new CassandraSession[F] {

    override val unwrap: CqlSession = session

    override val keyspace: Option[CqlIdentifier] = session.getKeyspace.toScala

    override def execute(query: String): F[AsyncResultSet] = {
      FromCompletionStage { session.executeAsync(query) }
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

    override val metadata: F[Metadata] = Async[F].delay { session.getMetadata }
  }

  /**
   * Creates a session with the given configuration, logging the configuration warnings,
   * if any.
   */
  def make[F[_]: Async: LogOf](config: CassandraConfig): Resource[F, CassandraSession[F]] = {
    for {
      log <- Resource.eval(LogOf[F].apply(CassandraSession.getClass))
      _ <- Resource.eval(config.warnings.traverse_ { warning => log.warn(warning) })
      builder <- Resource.eval(Async[F].delay { CqlSession.builder().withConfigLoader(config.configLoader) })
      session <- make(builder)
    } yield session
  }

  /**
   * Creates a session with the given builder.
   *
   * Use it to register custom codecs, listeners, request trackers, authentication or SSL
   * providers, or a custom configuration loader.
   */
  def make[F[_]: Async](builder: CqlSessionBuilder): Resource[F, CassandraSession[F]] = {
    // creation of a session is not cancelled to avoid leaking a partially initialized session
    val session = Async[F].uncancelable { _ => FromCompletionStage { builder.buildAsync() } }
    Resource
      .make(session) { session => FromCompletionStage { session.closeAsync() }.void }
      .map { session => CassandraSession[F](session) }
  }

  implicit class CassandraSessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G): CassandraSession[G] = new CassandraSession[G] {

      override def unwrap: CqlSession = self.unwrap

      override def keyspace: Option[CqlIdentifier] = self.keyspace

      override def execute(query: String): G[AsyncResultSet] = f(self.execute(query))

      override def execute(statement: Statement[?]): G[AsyncResultSet] = f(self.execute(statement))

      override def prepare(query: String): G[PreparedStatement] = f(self.prepare(query))

      override def prepare(statement: SimpleStatement): G[PreparedStatement] = f(self.prepare(statement))

      override def metadata: G[Metadata] = f(self.metadata)
    }
  }
}

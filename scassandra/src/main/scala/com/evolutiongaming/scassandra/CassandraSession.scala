package com.evolutiongaming.scassandra


import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import cats.~>
import com.datastax.oss.driver.api.core.cql.{AsyncCqlSession as SessionJ, *}
import com.evolutiongaming.util.ToJava


/**
  * See [[com.datastax.oss.driver.api.core.cql.AsyncCqlSession]]
  */
trait CassandraSession[F[_]] {
  def loggedKeyspace: F[Option[String]]

  def execute(query: String): F[AsyncResultSet]

  def execute(query: String, values: Any*): F[AsyncResultSet]

  def execute(query: String, values: Map[String, AnyRef]): F[AsyncResultSet]

  def execute(statement: Statement[?]): F[AsyncResultSet]

  def prepare(query: String): F[PreparedStatement]

  def prepare(statement: SimpleStatement): F[PreparedStatement]
}

trait CassandraSessionRaw[F[_]] extends CassandraSession[F] {
  def underlying: SessionJ
}

object CassandraSession {
  import com.evolutiongaming.scassandra.util.FromCompletionStage
  
  def apply[F[_]: Async](session: SessionJ): CassandraSessionRaw[F] = new Impl(session)

  private class Impl[F[_]: Async](val underlying: SessionJ) extends CassandraSessionRaw[F] {
    val loggedKeyspace: F[Option[String]] = {
      for {
        loggedKeyspace <- Sync[F].delay { underlying.getKeyspace }
      } yield {
        Option(loggedKeyspace)
      }
    }

    def execute(query: String): F[AsyncResultSet] =
      FromCompletionStage { underlying.executeAsync(query) }

    def execute(query: String, values: Any*): F[AsyncResultSet] =
      FromCompletionStage { underlying.executeAsync(query, values) }

    def execute(query: String, values: Map[String, AnyRef]): F[AsyncResultSet] = {
      val values1 = ToJava.from(values)
      FromCompletionStage { underlying.executeAsync(query, values1) }
    }

    def execute(statement: Statement[?]): F[AsyncResultSet] =
      FromCompletionStage { underlying.executeAsync(statement) }

    def prepare(query: String): F[PreparedStatement] =
      FromCompletionStage { underlying.prepareAsync(query) }

    def prepare(statement: SimpleStatement): F[PreparedStatement] =
      FromCompletionStage { underlying.prepareAsync(statement) }
  }

  def of[F[_]: Async](session: F[SessionJ]): Resource[F, CassandraSession[F]] = {
    val result = for {
      session <- session
    } yield {
      val release = FromCompletionStage { session.closeAsync() }.void
      (CassandraSession[F](session), release)
    }
    Resource(result)
  }
  
  implicit class SessionOps[F[_]](private val self: CassandraSession[F]) extends AnyVal {
    def mapK[G[_]](f: F ~> G): CassandraSession[G] = new CassandraSession[G] {
      def loggedKeyspace: G[Option[String]] = f(self.loggedKeyspace)

      def execute(query: String): G[AsyncResultSet] = f(self.execute(query))

      def execute(query: String, values: Any*): G[AsyncResultSet] = f(self.execute(query, values: _*))

      def execute(query: String, values: Map[String, AnyRef]): G[AsyncResultSet] = f(self.execute(query, values))

      def execute(statement: Statement[?]): G[AsyncResultSet] = f(self.execute(statement))

      def prepare(query: String): G[PreparedStatement] = f(self.prepare(query))

      def prepare(statement: SimpleStatement): G[PreparedStatement] = f(self.prepare(statement))
    }
  }
}

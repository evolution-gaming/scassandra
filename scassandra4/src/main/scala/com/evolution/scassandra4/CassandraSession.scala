package com.evolution.scassandra4

import cats.effect.Sync
import cats.syntax.all._
import cats.~>
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, PreparedStatement, SimpleStatement, Statement}
import com.datastax.oss.driver.api.core.metadata.Metadata
import com.evolution.scassandra4.util.FromCompletionStage

import scala.jdk.javaapi.OptionConverters

/** See [[com.datastax.oss.driver.api.core.CqlSession]] */
trait CassandraSession[F[_]] {

  def loggedKeyspace: F[Option[String]]

  def execute(query: String): F[AsyncResultSet]

  def execute(statement: Statement[?]): F[AsyncResultSet]

  def prepare(query: String): F[PreparedStatement]

  def prepare(statement: SimpleStatement): F[PreparedStatement]

  def metadata: F[Metadata]

  def clusterName: F[Option[String]]
}

object CassandraSession {

  def apply[F[_]](implicit F: CassandraSession[F]): CassandraSession[F] = F


  def apply[F[_]: Sync: FromCompletionStage](session: CqlSession): CassandraSession[F] = {

    new CassandraSession[F] {

      val loggedKeyspace = {
        Sync[F].delay {
          OptionConverters.toScala(session.getKeyspace).map(_.asInternal)
        }
      }

      def execute(query: String) = {
        FromCompletionStage[F].apply { session.executeAsync(query) }
      }

      def execute(statement: Statement[?]) = {
        FromCompletionStage[F].apply { session.executeAsync(statement) }
      }

      def prepare(query: String) = {
        FromCompletionStage[F].apply { session.prepareAsync(query) }
      }

      def prepare(statement: SimpleStatement) = {
        FromCompletionStage[F].apply { session.prepareAsync(statement) }
      }

      val metadata = Sync[F].delay { session.getMetadata }

      val clusterName = {
        for {
          metadata <- metadata
        } yield {
          OptionConverters.toScala(metadata.getClusterName)
        }
      }
    }
  }


  implicit class CassandraSessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G): CassandraSession[G] = {
      new CassandraSession[G] {

        def loggedKeyspace = f(self.loggedKeyspace)

        def execute(query: String) = f(self.execute(query))

        def execute(statement: Statement[?]) = f(self.execute(statement))

        def prepare(query: String) = f(self.prepare(query))

        def prepare(statement: SimpleStatement) = f(self.prepare(statement))

        def metadata = f(self.metadata)

        def clusterName = f(self.clusterName)
      }
    }
  }
}

package com.evolutiongaming.scassandra

import cats.effect.Async
import cats.effect.syntax.spawn.*
import cats.syntax.all.*
import com.datastax.oss.driver.api.core.cql.*
import com.evolutiongaming.scassandra.util.FromCompletionStage
import com.evolutiongaming.sstream.FoldWhile.FoldWhileOps
import com.evolutiongaming.sstream.Stream

import scala.jdk.CollectionConverters.IterableHasAsScala

object StreamingCassandraSession {
  implicit final class StreamingCassandraSessionOps[F[_]](private val self: CassandraSession[F]) extends AnyVal {
    def executeStream(statement: Statement[?])(implicit F: Async[F]): Stream[F, Row] = {
      for {
        resultSet <- Stream.lift(self.execute(statement))
        row       <- toStream(resultSet)
      } yield row
    }

    def executeStream(statement: String)(implicit F: Async[F]): Stream[F, Row] = executeStream(SimpleStatement.newInstance(statement))
  }

  private[scassandra] def toStream[F[_]: Async](resultSet: AsyncResultSet): Stream[F, Row] = {
    val fetch: F[Unit] = FromCompletionStage(resultSet.fetchNextPage()).void
    val fetched: F[Boolean] = Async[F].delay(!resultSet.hasMorePages)
    val next: F[Vector[Row]] = Async[F].delay(resultSet.currentPage().asScala.toVector)

    new Stream[F, Row] {
      def foldWhileM[L, R](l: L)(f: (L, Row) => F[Either[L, R]]): F[Either[L, R]] = {
        l.tailRecM[F, Either[L, R]] { l =>
          def apply(rows: Vector[Row]): F[Either[L, Either[L, R]]] = {
            rows.foldWhileM(l)(f).map(_.asRight[L])
          }

          def fetchAndApply(rows: Vector[Row]): F[Either[L, Either[L, R]]] = {
            for {
              fetching <- fetch.start
              result   <- rows.foldWhileM(l)(f)
              result   <- result match {
                case l: Left[L, R]  => fetching.joinWithNever.as(l.rightCast[Either[L, R]])
                case r: Right[L, R] => r.leftCast[L].asRight[L].pure[F]
              }
            } yield result
          }

          for {
            fetched <- fetched
            rows    <- next
            result  <- if (fetched) apply(rows) else fetchAndApply(rows)
          } yield result
        }
      }
    }
  }
}

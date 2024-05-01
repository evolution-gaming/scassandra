package com.evolutiongaming.scassandra

import cats.effect.Async
import cats.effect.syntax.spawn.*
import cats.syntax.all.*
import com.datastax.driver.core.{ResultSet, Row, SimpleStatement, Statement}
import com.evolutiongaming.scassandra.util.FromGFuture
import com.evolutiongaming.sstream.FoldWhile.FoldWhileOps
import com.evolutiongaming.sstream.Stream

object StreamingCassandraSession {
  implicit final class StreamingCassandraSessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {
    def executeStream(statement: Statement)(implicit F: Async[F]): Stream[F, Row] = {
      for {
        resultSet <- Stream.lift(self.execute(statement))
        row       <- toStream(resultSet)
      } yield row
    }

    def executeStream(statement: String)(implicit F: Async[F]): Stream[F, Row] = executeStream(new SimpleStatement(statement))
  }

  private def toStream[F[_]: Async](resultSet: ResultSet): Stream[F, Row] = {
    val iterator: java.util.Iterator[Row] = resultSet.iterator()
    val fetch: F[Unit] = FromGFuture[F].apply(resultSet.fetchMoreResults()).void
    val fetched: F[Boolean] = Async[F].delay(resultSet.isFullyFetched)
    val next: F[List[Row]] = Async[F].delay(List.fill(resultSet.getAvailableWithoutFetching)(iterator.next()))

    new Stream[F, Row] {
      def foldWhileM[L, R](l: L)(f: (L, Row) => F[Either[L, R]]): F[Either[L, R]] = {
        l.tailRecM[F, Either[L, R]] { l =>
          def apply(rows: List[Row]): F[Either[L, Either[L, R]]] = {
            rows.foldWhileM(l)(f).map(_.asRight[L])
          }

          def fetchAndApply(rows: List[Row]): F[Either[L, Either[L, R]]] = {
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

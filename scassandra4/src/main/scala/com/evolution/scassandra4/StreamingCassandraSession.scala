package com.evolution.scassandra4

import cats.effect.Async
import cats.effect.syntax.spawn.*
import cats.syntax.all.*
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, Row, SimpleStatement, Statement}
import com.evolution.scassandra4.util.FromCompletionStage
import com.evolutiongaming.sstream.FoldWhile.FoldWhileOps
import com.evolutiongaming.sstream.Stream

import scala.jdk.CollectionConverters._

object StreamingCassandraSession {

  implicit final class StreamingCassandraSessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def executeStream(statement: Statement[?])(implicit F: Async[F], fromCompletionStage: FromCompletionStage[F]): Stream[F, Row] = {
      for {
        resultSet <- Stream.lift(self.execute(statement))
        row       <- toStream(resultSet)
      } yield row
    }

    def executeStream(statement: String)(implicit F: Async[F], fromCompletionStage: FromCompletionStage[F]): Stream[F, Row] = {
      executeStream(SimpleStatement.newInstance(statement))
    }
  }

  /** Streams all remaining rows of an [[AsyncResultSet]], page by page.
    *
    * While a page is being folded, the next page is already being fetched in
    * the background (the driver 3 based module used `fetchMoreResults` for the
    * same effect).
    */
  def toStream[F[_]: Async: FromCompletionStage](resultSet: AsyncResultSet): Stream[F, Row] = {

    new Stream[F, Row] {

      def foldWhileM[L, R](l: L)(f: (L, Row) => F[Either[L, R]]): F[Either[L, R]] = {

        (resultSet, l).tailRecM[F, Either[L, R]] { case (resultSet, l) =>

          def rows: F[List[Row]] = Async[F].delay {
            resultSet.currentPage().iterator().asScala.toList
          }

          def lastPage(rows: List[Row]): F[Either[(AsyncResultSet, L), Either[L, R]]] = {
            rows.foldWhileM(l)(f).map { _.asRight[(AsyncResultSet, L)] }
          }

          def fetchAndApply(rows: List[Row]): F[Either[(AsyncResultSet, L), Either[L, R]]] = {
            for {
              fetching <- FromCompletionStage[F].apply { resultSet.fetchNextPage() }.start
              result   <- rows.foldWhileM(l)(f)
              result   <- result match {
                case Left(l)        =>
                  fetching.joinWithNever.map { resultSet => (resultSet, l).asLeft[Either[L, R]] }
                case r: Right[L, R] =>
                  fetching.cancel.as((r: Either[L, R]).asRight[(AsyncResultSet, L)])
              }
            } yield result
          }

          for {
            rows     <- rows
            hasMore  <- Async[F].delay { resultSet.hasMorePages }
            result   <- if (hasMore) fetchAndApply(rows) else lastPage(rows)
          } yield result
        }
      }
    }
  }
}

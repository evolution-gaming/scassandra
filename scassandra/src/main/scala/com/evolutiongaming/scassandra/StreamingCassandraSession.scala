package com.evolutiongaming.scassandra

import cats.effect.Async
import cats.effect.syntax.spawn.*
import cats.syntax.all.*
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, Row, SimpleStatement, Statement}
import com.evolutiongaming.scassandra.util.FromCompletionStage
import com.evolutiongaming.sstream.FoldWhile.FoldWhileOps
import com.evolutiongaming.sstream.Stream

import scala.jdk.CollectionConverters.*

object StreamingCassandraSession {

  implicit final class StreamingCassandraSessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def executeStream(
      statement: Statement[?],
    )(implicit
      F: Async[F],
    ): Stream[F, Row] = {
      for {
        resultSet <- Stream.lift(self.execute(statement))
        row <- toStream(resultSet)
      } yield row
    }

    def executeStream(
      statement: String,
    )(implicit
      F: Async[F],
    ): Stream[F, Row] = executeStream(SimpleStatement.newInstance(statement))
  }

  /**
   * Streams the rows of all the pages, the next page is fetched while the current one is
   * being processed. The first page is read from `resultSet` once and kept, so the stream
   * can be run more than once.
   */
  private[scassandra] def toStream[F[_]: Async](resultSet: AsyncResultSet): Stream[F, Row] = {
    new Stream[F, Row] {

      private lazy val firstPage = rowsOf(resultSet)

      def foldWhileM[L, R](l: L)(f: (L, Row) => F[Either[L, R]]): F[Either[L, R]] = {
        (l, firstPage, resultSet).tailRecM[F, Either[L, R]] { case (l, rows, resultSet) =>
          val fold = rows.foldWhileM(l)(f)
          if (resultSet.hasMorePages) {
            FromCompletionStage { resultSet.fetchNextPage() }.background.use { next =>
              fold.flatMap {
                case Left(l) =>
                  next.flatMap(_.embedNever).map { next => (l, rowsOf(next), next).asLeft[Either[L, R]] }
                case result => result.asRight[(L, List[Row], AsyncResultSet)].pure[F]
              }
            }
          } else {
            fold.map(_.asRight[(L, List[Row], AsyncResultSet)])
          }
        }
      }
    }
  }

  private def rowsOf(resultSet: AsyncResultSet): List[Row] = resultSet.currentPage().asScala.toList
}

package com.evolutiongaming.scassandra

import cats.effect.Async
import cats.effect.syntax.spawn._
import cats.syntax.all._
import cats.~>
import com.evolutiongaming.scassandra.util.FromGFuture
import com.evolutiongaming.sstream.Stream
import com.evolutiongaming.sstream.FoldWhile.FoldWhileOps
import com.datastax.driver.core.{RegularStatement, ResultSet, Row, SimpleStatement, Statement}

/** 
 *  A `CassandraSession` that supports streaming results from Cassandra using `sstream.Stream`.
 */
trait StreamingCassandraSession[F[_]] extends CassandraSession[F] {
  def executeStream(statement: Statement): Stream[F, Row]

  final def executeStream(statement: String): Stream[F, Row] = executeStream(new SimpleStatement(statement))
}

object StreamingCassandraSession {
  /** 
   *  Wrap a `CassandraSession` in a `StreamingCassandraSession`.
   *  Creates a thin wrapper around the `CassandraSession` that induces no overhead
   *  except for the memory footprint of the wrapper itself and doesn't allocate any additional 
   *  resources.
   */
  def of[F[_]: Async](session: CassandraSession[F]): StreamingCassandraSession[F] = {
    new StreamingCassandraSession[F] {
      def loggedKeyspace = session.loggedKeyspace

      def init = session.init

      def execute(query: String) = session.execute(query)

      def execute(query: String, values: Any*) = session.execute(query, values: _*)

      def execute(query: String, values: Map[String, AnyRef]) = session.execute(query, values)

      def execute(statement: Statement) = session.execute(statement)

      def prepare(query: String) = session.prepare(query)

      def prepare(statement: RegularStatement) = session.prepare(statement)

      def state = session.state

      def executeStream(statement: Statement) = {
        for {
          resultSet <- Stream.lift(execute(statement))
          row       <- toStream(resultSet)
        } yield row
      }

      private def toStream(resultSet: ResultSet): Stream[F, Row] = {
        val iterator = resultSet.iterator()
        val fetch = FromGFuture[F].apply(resultSet.fetchMoreResults()).void 
        val fetched = Async[F].delay(resultSet.isFullyFetched)
        val next = Async[F].delay(List.fill(resultSet.getAvailableWithoutFetching)(iterator.next()))

        new Stream[F, Row] {
          def foldWhileM[L, R](l: L)(f: (L, Row) => F[Either[L, R]]) = {
            l.tailRecM[F, Either[L, R]] { l =>
              def apply(rows: List[Row]) = {
                for {
                  result <- rows.foldWhileM(l)(f)
                } yield {
                  result.asRight[L]
                }
              }

              def fetchAndApply(rows: List[Row]) = {
                for {
                  fetching <- fetch.start
                  result   <- rows.foldWhileM(l)(f)
                  result   <- result match {
                    case l: Left[L, R]  => fetching.joinWithNever as l.rightCast[Either[L, R]]
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
  }

  implicit class StreamingSessionOps[F[_]](val self: StreamingCassandraSession[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G, g: G ~> F): StreamingCassandraSession[G] = new StreamingCassandraSession[G] {

      def loggedKeyspace = f(self.loggedKeyspace)

      def init = f(self.init)

      def execute(query: String) = f(self.execute(query))

      def execute(query: String, values: Any*) = f(self.execute(query, values: _*))

      def execute(query: String, values: Map[String, AnyRef]) = f(self.execute(query, values))

      def execute(statement: Statement) = f(self.execute(statement))

      def executeStream(statement: Statement) = self.executeStream(statement).mapK(f, g)

      def prepare(query: String) = f(self.prepare(query))

      def prepare(statement: RegularStatement) = f(self.prepare(statement))

      def state = self.state.mapK(f)
    }
  }
}

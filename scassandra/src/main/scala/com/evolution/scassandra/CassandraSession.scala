package com.evolution.scassandra

import cats.effect.{Resource, Async}
import cats.effect.syntax.spawn._
import cats.implicits._
import cats.~>
import com.datastax.driver.core.{Session => SessionJ, _}
import com.evolutiongaming.scassandra.util.FromGFuture
import com.evolutiongaming.sstream.Stream
import com.evolutiongaming.sstream.FoldWhile.FoldWhileOps
import com.evolutiongaming.util.{ToScala, ToJava}

/**
  * A wrapper around the `com.datastax.driver.core.Session` interface that provides a more
  * functional interface for interacting with Cassandra.
  *
  * This is a newer implementation of [[com.evolutiongaming.scassandra.CassandraSession]] 
  * with the only major difference being the addition of the `executeStream` method that returns a 
  * `Stream` rather than a raw `ResultSet`.
  * 
  * @see [[com.datastax.driver.core.Session]] the underlying Java driver session interface
  * @see [[com.evolutiongaming.scassandra.CassandraSession]] the original implementation
  */
trait CassandraSession[F[_]] {

  def loggedKeyspace: F[Option[String]]

  def init: F[Unit]

  def execute(query: String): F[ResultSet]

  def execute(query: String, values: Any*): F[ResultSet]

  def execute(query: String, values: Map[String, AnyRef]): F[ResultSet]

  def execute(statement: Statement): F[ResultSet]

  def executeStream(statement: Statement): Stream[F, Row]

  final def executeStream(statement: String): Stream[F, Row] = executeStream(new SimpleStatement(statement))

  def prepare(query: String): F[PreparedStatement]

  def prepare(statement: RegularStatement): F[PreparedStatement]

  def state: CassandraSession.State[F]
}

object CassandraSession {

  def apply[F[_]](implicit F: CassandraSession[F]): CassandraSession[F] = F


  def apply[F[_] : Async](session: SessionJ): CassandraSession[F] = {

    new CassandraSession[F] {

      val loggedKeyspace = {
        for {
          loggedKeyspace <- Async[F].delay { session.getLoggedKeyspace }
        } yield {
          Option(loggedKeyspace)
        }
      }

      val init = FromGFuture[F].apply { session.initAsync() }.void

      def execute(query: String) = {
        FromGFuture[F].apply { session.executeAsync(query) }
      }

      def execute(query: String, values: Any*) = {
        FromGFuture[F].apply { session.executeAsync(query, values) }
      }

      def execute(query: String, values: Map[String, AnyRef]) = {
        val values1 = ToJava.from(values)
        FromGFuture[F].apply { session.executeAsync(query, values1) }
      }

      def execute(statement: Statement) = {
        FromGFuture[F].apply { session.executeAsync(statement) }
      }

      def executeStream(statement: Statement): Stream[F,Row] = {
        for {
          resultSet <- Stream.lift(execute(statement))
          row       <- toStream(resultSet)
        } yield row
      }

      def prepare(query: String) = {
        FromGFuture[F].apply { session.prepareAsync(query) }
      }

      def prepare(statement: RegularStatement) = {
        FromGFuture[F].apply { session.prepareAsync(statement) }
      }

      val state = State[F](session.getState)

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


  def of[F[_]: Async](session: F[SessionJ]): Resource[F, CassandraSession[F]] = {
    val result = for {
      session <- session
    } yield {
      val release = FromGFuture[F].apply { session.closeAsync() }.void
      (CassandraSession[F](session), release)
    }
    Resource(result)
  }


  trait State[F[_]] {

    def connectedHosts: F[Iterable[Host]]

    def openConnections(host: Host): F[Int]

    def trashedConnections(host: Host): F[Int]

    def inFlightQueries(host: Host): F[Int]
  }

  object State {

    def apply[F[_] : Async](state: SessionJ.State): State[F] = {
      new State[F] {

        val connectedHosts = {
          for {
            a <- Async[F].delay { state.getConnectedHosts }
          } yield {
            ToScala.from(a)
          }
        }

        def openConnections(host: Host) = {
          Async[F].delay { state.getOpenConnections(host) }
        }

        def trashedConnections(host: Host) = {
          Async[F].delay { state.getTrashedConnections(host) }
        }

        def inFlightQueries(host: Host) = {
          Async[F].delay { state.getInFlightQueries(host) }
        }
      }
    }


    implicit class StateOps[F[_]](val self: State[F]) extends AnyVal {

      def mapK[G[_]](f: F ~> G): State[G] = new State[G] {

        def connectedHosts = f(self.connectedHosts)

        def openConnections(host: Host) = f(self.openConnections(host))

        def trashedConnections(host: Host) = f(self.trashedConnections(host))

        def inFlightQueries(host: Host) = f(self.inFlightQueries(host))
      }
    }
  }


  implicit class SessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G, g: G ~> F): CassandraSession[G] = new CassandraSession[G] {

      def loggedKeyspace = f(self.loggedKeyspace)

      def init = f(self.init)

      def execute(query: String) = f(self.execute(query))

      def execute(query: String, values: Any*) = f(self.execute(query, values: _*))

      def execute(query: String, values: Map[String, AnyRef]) = f(self.execute(query, values))

      def execute(statement: Statement) = f(self.execute(statement))

      def executeStream(statement: Statement): Stream[G, Row] = self.executeStream(statement).mapK(f, g)

      def prepare(query: String) = f(self.prepare(query))

      def prepare(statement: RegularStatement) = f(self.prepare(statement))

      def state = self.state.mapK(f)
    }
  }
}

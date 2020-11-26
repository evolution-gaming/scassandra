package com.evolutiongaming.scassandra


import cats.effect.{Resource, Sync}
import cats.syntax.all._
import cats.~>
import com.datastax.driver.core.{Session => SessionJ, _}
import com.evolutiongaming.scassandra.util.FromGFuture
import com.evolutiongaming.util.{ToScala, ToJava}

/**
  * See [[com.datastax.driver.core.Session]]
  */
trait CassandraSession[F[_]] {

  def loggedKeyspace: F[Option[String]]

  def init: F[Unit]

  def execute(query: String): F[ResultSet]

  def execute(query: String, values: Any*): F[ResultSet]

  def execute(query: String, values: Map[String, AnyRef]): F[ResultSet]

  def execute(statement: Statement): F[ResultSet]

  def prepare(query: String): F[PreparedStatement]

  def prepare(statement: RegularStatement): F[PreparedStatement]

  def state: CassandraSession.State[F]
}

object CassandraSession {

  def apply[F[_]](implicit F: CassandraSession[F]): CassandraSession[F] = F


  def apply[F[_] : Sync : FromGFuture](session: SessionJ): CassandraSession[F] = {

    new CassandraSession[F] {

      val loggedKeyspace = {
        for {
          loggedKeyspace <- Sync[F].delay { session.getLoggedKeyspace }
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

      def prepare(query: String) = {
        FromGFuture[F].apply { session.prepareAsync(query) }
      }

      def prepare(statement: RegularStatement) = {
        FromGFuture[F].apply { session.prepareAsync(statement) }
      }

      val state = State[F](session.getState)
    }
  }


  def of[F[_] : Sync : FromGFuture](session: F[SessionJ]): Resource[F, CassandraSession[F]] = {
    val result = for {
      session <- session
    } yield {
      val release = FromGFuture[F].apply { session.closeAsync() }.void
      (CassandraSession[F](session), release)
    }
    Resource(result)
  }


  /**
    * See [[com.evolutiongaming.scassandra.CassandraSession.State]]
    */
  trait State[F[_]] {

    def connectedHosts: F[Iterable[Host]]

    def openConnections(host: Host): F[Int]

    def trashedConnections(host: Host): F[Int]

    def inFlightQueries(host: Host): F[Int]
  }

  object State {

    def apply[F[_] : Sync](state: SessionJ.State): State[F] = {
      new State[F] {

        val connectedHosts = {
          for {
            a <- Sync[F].delay { state.getConnectedHosts }
          } yield {
            ToScala.from(a)
          }
        }

        def openConnections(host: Host) = {
          Sync[F].delay { state.getOpenConnections(host) }
        }

        def trashedConnections(host: Host) = {
          Sync[F].delay { state.getTrashedConnections(host) }
        }

        def inFlightQueries(host: Host) = {
          Sync[F].delay { state.getInFlightQueries(host) }
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

    def mapK[G[_]](f: F ~> G): CassandraSession[G] = new CassandraSession[G] {

      def loggedKeyspace = f(self.loggedKeyspace)

      def init = f(self.init)

      def execute(query: String) = f(self.execute(query))

      def execute(query: String, values: Any*) = f(self.execute(query, values: _*))

      def execute(query: String, values: Map[String, AnyRef]) = f(self.execute(query, values))

      def execute(statement: Statement) = f(self.execute(statement))

      def prepare(query: String) = f(self.prepare(query))

      def prepare(statement: RegularStatement) = f(self.prepare(statement))

      def state = self.state.mapK(f)
    }
  }
}

package com.evolutiongaming.scassandra


import cats.effect.{Resource, Sync}
import cats.implicits._
import cats.~>
import com.datastax.driver.core.{Session => SessionJ, _}
import com.evolutiongaming.scassandra.util.FromGFuture

import scala.collection.JavaConverters._

/**
  * See [[com.datastax.driver.core.Session]]
  */
trait Session[F[_]] {

  def loggedKeyspace: F[Option[String]]

  def init: F[Unit]

  def execute(query: String): F[ResultSet]

  def execute(query: String, values: Any*): F[ResultSet]

  def execute(query: String, values: Map[String, AnyRef]): F[ResultSet]

  def execute(statement: Statement): F[ResultSet]

  def prepare(query: String): F[PreparedStatement]

  def prepare(statement: RegularStatement): F[PreparedStatement]

  def state: Session.State[F]
}

object Session {

  def apply[F[_] : Sync : FromGFuture](session: SessionJ): Session[F] = {

    new Session[F] {

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
        val values1 = values.asJava
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


  def of[F[_] : Sync : FromGFuture](session: F[SessionJ]): Resource[F, Session[F]] = {
    val result = for {
      session <- session
    } yield {
      val release = FromGFuture[F].apply { session.closeAsync() }.void
      (Session[F](session), release)
    }
    Resource(result)
  }


  /**
    * See [[com.evolutiongaming.scassandra.Session.State]]
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
            a.asScala
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


  implicit class SessionOps[F[_]](val self: Session[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G): Session[G] = new Session[G] {

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

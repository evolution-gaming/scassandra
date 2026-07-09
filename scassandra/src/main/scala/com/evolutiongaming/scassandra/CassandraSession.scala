package com.evolutiongaming.scassandra

import cats.effect.{Resource, Sync}
import cats.implicits.*
import cats.~>
import com.datastax.driver.core.{Session as SessionJ, *}
import com.evolutiongaming.scassandra.util.FromGFuture

import scala.jdk.CollectionConverters.*

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

  def apply[F[_]](
    implicit
    F: CassandraSession[F],
  ): CassandraSession[F] = F

  def apply[F[_]: Sync: FromGFuture](session: SessionJ): CassandraSession[F] = {

    new CassandraSession[F] {

      override val loggedKeyspace: F[Option[String]] = Sync[F].delay { Option(session.getLoggedKeyspace) }

      override val init: F[Unit] = FromGFuture[F].apply { session.initAsync() }.void

      override def execute(query: String): F[ResultSet] = {
        FromGFuture[F].apply { session.executeAsync(query) }
      }

      // [potential major bug]
      // TODO [AI review] `values` is passed to the Java varargs `executeAsync(String, Object...)`
      // as a single Seq argument instead of being expanded with `values*`, so all positional bound
      // values collapse into one value and the statement fails at runtime. This overload is not
      // covered by any test.
      override def execute(query: String, values: Any*): F[ResultSet] = {
        FromGFuture[F].apply { session.executeAsync(query, values) }
      }

      override def execute(query: String, values: Map[String, AnyRef]): F[ResultSet] = {
        FromGFuture[F].apply { session.executeAsync(query, values.asJava) }
      }

      override def execute(statement: Statement): F[ResultSet] = {
        FromGFuture[F].apply { session.executeAsync(statement) }
      }

      override def prepare(query: String): F[PreparedStatement] = {
        FromGFuture[F].apply { session.prepareAsync(query) }
      }

      override def prepare(statement: RegularStatement): F[PreparedStatement] = {
        FromGFuture[F].apply { session.prepareAsync(statement) }
      }

      // [major bug]
      // TODO [AI review] `session.getState` returns an immutable point-in-time snapshot and is
      // called eagerly here, so later reads of connectedHosts/openConnections/inFlightQueries return
      // frozen construction-time values; the side-effecting getState() also runs unsuspended outside F.
      // Make this a `def` (or suspend the getState() call) so each access takes a fresh snapshot.
      override val state: State[F] = State[F](session.getState)
    }
  }

  def of[F[_]: Sync: FromGFuture](session: F[SessionJ]): Resource[F, CassandraSession[F]] = {
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

    def apply[F[_]: Sync](state: SessionJ.State): State[F] = {
      new State[F] {

        override val connectedHosts: F[Iterable[Host]] = Sync[F].delay { state.getConnectedHosts.asScala }

        override def openConnections(host: Host): F[Int] = {
          Sync[F].delay { state.getOpenConnections(host) }
        }

        override def trashedConnections(host: Host): F[Int] = {
          Sync[F].delay { state.getTrashedConnections(host) }
        }

        override def inFlightQueries(host: Host): F[Int] = {
          Sync[F].delay { state.getInFlightQueries(host) }
        }
      }
    }

    implicit class StateOps[F[_]](val self: State[F]) extends AnyVal {

      def mapK[G[_]](f: F ~> G): State[G] = new State[G] {

        override def connectedHosts: G[Iterable[Host]] = f(self.connectedHosts)

        override def openConnections(host: Host): G[Int] = f(self.openConnections(host))

        override def trashedConnections(host: Host): G[Int] = f(self.trashedConnections(host))

        override def inFlightQueries(host: Host): G[Int] = f(self.inFlightQueries(host))
      }
    }
  }

  implicit class SessionOps[F[_]](val self: CassandraSession[F]) extends AnyVal {

    def mapK[G[_]](f: F ~> G): CassandraSession[G] = new CassandraSession[G] {

      override def loggedKeyspace: G[Option[String]] = f(self.loggedKeyspace)

      override def init: G[Unit] = f(self.init)

      override def execute(query: String): G[ResultSet] = f(self.execute(query))

      override def execute(query: String, values: Any*): G[ResultSet] = f(self.execute(query, values*))

      override def execute(query: String, values: Map[String, AnyRef]): G[ResultSet] =
        f(self.execute(query, values))

      override def execute(statement: Statement): G[ResultSet] = f(self.execute(statement))

      override def prepare(query: String): G[PreparedStatement] = f(self.prepare(query))

      override def prepare(statement: RegularStatement): G[PreparedStatement] = f(self.prepare(statement))

      override def state: State[G] = self.state.mapK(f)
    }
  }
}

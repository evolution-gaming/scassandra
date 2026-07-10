package com.evolutiongaming.scassandra

import cats.effect.{Resource, Sync}
import cats.implicits.*
import cats.~>
import com.datastax.driver.core.{Session as SessionJ, *}
import com.evolutiongaming.scassandra.CassandraSession.StateSnapshot
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

  /**
   * Deprecated method.
   *
   * Pre 5.6.0 implementation was incorrect. It was fixed in 5.6.0 but a better way to
   * access this functionality was added, which is semantically closer to what the Java
   * driver offers. Please use [[CassandraSession.stateSnapshot]] instead.
   *
   * The original bug description:
   * {{{
   * // TODO `session.getState` returns an immutable point-in-time snapshot and is
   * // called eagerly here, so later reads of connectedHosts/openConnections/inFlightQueries return
   * // frozen construction-time values; the side-effecting getState() also runs unsuspended outside F.
   * // Make this a `def` (or suspend the getState() call) so each access takes a fresh snapshot.
   * }}}
   */
  @deprecated("use stateSnapshot instead", since = "5.6.0")
  def state: CassandraSession.State[F]

  /**
   * @see
   *   [[com.datastax.driver.core.Session.getState]]
   */
  def stateSnapshot: F[StateSnapshot] = {
    // default impl to preserve bincompat
    // TODO: remove in a major release > 5
    ???
  }
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

      override def execute(query: String, values: Any*): F[ResultSet] = {
        FromGFuture[F].apply { session.executeAsync(query, values*) }
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

      override val stateSnapshot: F[StateSnapshot] = Sync[F].delay {
        // com.datastax.driver.core.Session.getState return value is guaranteed to be immutable
        StateSnapshot.wrapImmutable(session.getState)
      }

      // see the trait method scaladoc for more details
      @deprecated("use stateSnapshot instead", since = "5.6.0")
      override val state: State[F] = State.suspended(stateSnapshot)
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
   * Immutable object exposing information on the connections maintained by a Session:
   * which host it is connected to, how many connections it has for each host, etc...
   *
   * Scala wrapper around [[com.datastax.driver.core.Session.State]], assuming the Java
   * implementation is immutable, which is the case for the values returned by
   * [[com.datastax.driver.core.Session.getState]].
   */
  trait StateSnapshot {
    def connectedHosts: Iterable[Host]

    def openConnections(host: Host): Int

    def trashedConnections(host: Host): Int

    def inFlightQueries(host: Host): Int
  }

  object StateSnapshot {

    /**
     * Wraps [[com.datastax.driver.core.Session.State]] in [[StateSnapshot]] assuming it
     * is immutable.
     *
     * Immutability is guaranteed by the [[com.datastax.driver.core.Session.getState]]
     * contract but using this method in other contexts might be unsafe.
     */
    def wrapImmutable(driverState: SessionJ.State): StateSnapshot = new Impl(driverState)

    private final class Impl(driverState: SessionJ.State) extends StateSnapshot {
      override val connectedHosts: Iterable[Host] = driverState.getConnectedHosts.asScala

      override def openConnections(host: Host): Int = driverState.getOpenConnections(host)

      override def trashedConnections(host: Host): Int = driverState.getTrashedConnections(host)

      override def inFlightQueries(host: Host): Int = driverState.getInFlightQueries(host)
    }
  }

  @deprecated("replaced by StateSnapshot, returned by CassandraSession.stateSnapshot", since = "5.6.0")
  trait State[F[_]] {

    def connectedHosts: F[Iterable[Host]]

    def openConnections(host: Host): F[Int]

    def trashedConnections(host: Host): F[Int]

    def inFlightQueries(host: Host): F[Int]
  }

  @deprecated("replaced by StateSnapshot, returned by CassandraSession.stateSnapshot", since = "5.6.0")
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

    def suspended[F[_]: Sync](snapshotF: F[StateSnapshot]): State[F] = new Suspended[F](snapshotF)

    private final class Suspended[F[_]: Sync](snapshotF: F[StateSnapshot]) extends State[F] {
      override val connectedHosts: F[Iterable[Host]] = {
        snapshotF.map(_.connectedHosts)
      }

      override def openConnections(host: Host): F[Int] = {
        snapshotF.map(_.openConnections(host))
      }

      override def trashedConnections(host: Host): F[Int] = {
        snapshotF.map(_.trashedConnections(host))
      }

      override def inFlightQueries(host: Host): F[Int] = {
        snapshotF.map(_.inFlightQueries(host))
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

      // see the trait method scaladoc for more details
      @deprecated("use stateSnapshot instead", since = "5.6.0")
      override def state: State[G] = self.state.mapK(f)

      override def stateSnapshot: G[StateSnapshot] = f(self.stateSnapshot)
    }
  }
}

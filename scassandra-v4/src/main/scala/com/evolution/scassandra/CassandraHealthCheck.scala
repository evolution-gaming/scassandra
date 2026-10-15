package com.evolution.scassandra

import cats.Monad
import cats.effect.*
import cats.syntax.all.*
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.evolutiongaming.catshelper.{Log, LogOf, Schedule}

import scala.concurrent.duration.*

/**
 * Performs a check if Cassandra is alive.
 *
 * The common implementation is to periodically do a simple query and check if it returns
 * an error.
 */
trait CassandraHealthCheck[F[_]] {

  /**
   * @return
   *   `None` if Cassandra healthy, and `Some(error)` otherwise
   */
  def error: F[Option[Throwable]]
}

object CassandraHealthCheck {

  /**
   * Checks if Cassandra is alive by requesting a current timestamp from Cassandra.
   *
   * I.e., it does the following query every second after initial ramp-up delay of 10
   * seconds.
   * ```sql
   * SELECT now() FROM system.local
   * ```
   *
   * @param session
   *   Cassandra session to use to perform queries with.
   * @param consistencyLevel
   *   Read consistency level to use for a query.
   */
  def make[F[_]: Temporal: LogOf](
    session: Resource[F, CassandraSession[F]],
    consistencyLevel: ConsistencyLevel,
  ): Resource[F, CassandraHealthCheck[F]] = {

    val statement = for {
      session <- session
      statement <- Resource.eval(Statement.of[F](session, consistencyLevel))
    } yield statement

    for {
      log <- Resource.eval(LogOf[F].apply(CassandraHealthCheck.getClass))
      result <- make(initial = 10.seconds, interval = 1.second, statement = statement, log = log)
    } yield result
  }

  /**
   * Checks if server is alive by doing a custom `F[Unit]` call.
   *
   * @param initial
   *   Initial ramp-up delay before health checks are started.
   * @param interval
   *   How often the provided function should be called.
   * @param statement
   *   The function to call to check if server is alive. The function is expected to throw
   *   an error if server is not healthy.
   * @param log
   *   The log to write an error to, in addition to returning it in
   *   [[CassandraHealthCheck#error]] call.
   */
  def make[F[_]: Temporal](
    initial: FiniteDuration,
    interval: FiniteDuration,
    statement: Resource[F, Statement[F]],
    log: Log[F],
  ): Resource[F, CassandraHealthCheck[F]] = {
    for {
      ref <- Resource.eval(Ref.of[F, Option[Throwable]](none))
      statement <- statement
      _ <- Schedule(initial, interval) {
        for {
          maybeError <- statement.redeem(_.some, _ => none[Throwable])
          _ <- maybeError.foldMapM { error => log.error(s"failed with $error", error) }
          _ <- ref.set(maybeError)
        } yield ()
      }
    } yield new CassandraHealthCheck[F] {
      override def error: F[Option[Throwable]] = ref.get
    }
  }

  type Statement[F[_]] = F[Unit]

  object Statement {

    def of[F[_]: Monad](session: CassandraSession[F], consistencyLevel: ConsistencyLevel): F[Statement[F]] = {
      session.prepare("SELECT now() FROM system.local").map { prepared =>
        session.execute(prepared.bind().setConsistencyLevel(consistencyLevel)).void
      }
    }
  }
}

package com.evolutiongaming.scassandra

import cats.effect.IO
import com.datastax.driver.core.{PreparedStatement, RegularStatement, ResultSet, Statement}
import com.evolutiongaming.scassandra.MockSupport.notSupported

class CassandraSessionMock extends CassandraSession[IO] {

  def loggedKeyspace: IO[Option[String]] = notSupported

  def init: IO[Unit] = notSupported

  def execute(query: String): IO[ResultSet] = notSupported

  def execute(query: String, values: Any*): IO[ResultSet] = notSupported

  def execute(query: String, values: Map[String, AnyRef]): IO[ResultSet] = notSupported

  def execute(statement: Statement): IO[ResultSet] = notSupported

  def prepare(query: String): IO[PreparedStatement] = notSupported

  def prepare(statement: RegularStatement): IO[PreparedStatement] = notSupported

  @deprecated("use stateSnapshot instead", since = "5.6.0")
  def state: CassandraSession.State[IO] = notSupported

  override def stateSnapshot: IO[CassandraSession.StateSnapshot] = notSupported
}

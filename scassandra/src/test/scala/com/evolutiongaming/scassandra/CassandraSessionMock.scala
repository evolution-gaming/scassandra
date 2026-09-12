package com.evolutiongaming.scassandra

import cats.effect.IO
import com.datastax.driver.core.{PreparedStatement, RegularStatement, ResultSet, Statement}

class CassandraSessionMock extends CassandraSession[IO] {

  def loggedKeyspace: IO[Option[String]] = sys.error("not supported")

  def init: IO[Unit] = sys.error("not supported")

  def execute(query: String): IO[ResultSet] = sys.error("not supported")

  def execute(query: String, values: Any*): IO[ResultSet] = sys.error("not supported")

  def execute(query: String, values: Map[String, AnyRef]): IO[ResultSet] = sys.error("not supported")

  def execute(statement: Statement): IO[ResultSet] = sys.error("not supported")

  def prepare(query: String): IO[PreparedStatement] = sys.error("not supported")

  def prepare(statement: RegularStatement): IO[PreparedStatement] = sys.error("not supported")

  @deprecated("use stateSnapshot instead", since = "5.6.0")
  def state: CassandraSession.State[IO] = sys.error("not supported")
}

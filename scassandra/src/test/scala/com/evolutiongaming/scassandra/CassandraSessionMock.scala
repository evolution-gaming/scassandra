package com.evolutiongaming.scassandra

import cats.effect.IO
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, PreparedStatement, SimpleStatement, Statement}
import com.evolutiongaming.scassandra.MockSupport.notSupported

class CassandraSessionMock extends CassandraSession[IO] {

  def loggedKeyspace: IO[Option[String]] = notSupported

  def execute(query: String): IO[AsyncResultSet] = notSupported

  def execute(query: String, values: Any*): IO[AsyncResultSet] = notSupported

  def execute(query: String, values: Map[String, AnyRef]): IO[AsyncResultSet] = notSupported

  def execute(statement: Statement[?]): IO[AsyncResultSet] = notSupported

  def prepare(query: String): IO[PreparedStatement] = notSupported

  def prepare(statement: SimpleStatement): IO[PreparedStatement] = notSupported

  def metadata: IO[Metadata[IO]] = notSupported
}

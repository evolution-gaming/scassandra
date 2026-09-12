package com.evolutiongaming.scassandra

import cats.effect.IO
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.exceptions.{CodecNotFoundException, InvalidQueryException, SyntaxError}
import com.evolutiongaming.catshelper.CatsHelper.*
import com.evolutiongaming.scassandra.syntax.*
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SessionSpec extends AnyFunSuite with CassandraSuite with Matchers with EitherValues {

  private lazy val table = "session_spec"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val program = for {
      _ <- session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.$table (id INT PRIMARY KEY, value TEXT)")
      _ <- session.execute(s"INSERT INTO $keyspace.$table (id, value) VALUES (1, 'value')")
    } yield ()
    program.toTry.get
  }

  private def failure(io: IO[?]): Throwable = io.attempt.toTry.get.left.value

  test("query against a missing table fails with InvalidQueryException") {
    failure(session.execute(s"SELECT * FROM $keyspace.missing")) shouldBe a[InvalidQueryException]
  }

  test("malformed query fails with SyntaxError") {
    failure(session.execute("SELEC 1")) shouldBe a[SyntaxError]
  }

  test("prepare of an invalid query fails with InvalidQueryException") {
    failure(session.prepare(s"SELECT * FROM $keyspace.missing WHERE id = ?")) shouldBe
      a[InvalidQueryException]
  }

  test("decoding a missing column fails with IllegalArgumentException") {
    val row = session.execute(s"SELECT value FROM $keyspace.$table WHERE id = 1").toTry.get.one()
    an[IllegalArgumentException] should be thrownBy row.decode[String]("missing")
    an[IllegalArgumentException] should be thrownBy row.decode[Option[String]]("missing")
  }

  test("decoding a column as a wrong type fails with CodecNotFoundException") {
    val row = session.execute(s"SELECT value FROM $keyspace.$table WHERE id = 1").toTry.get.one()
    a[CodecNotFoundException] should be thrownBy row.decode[Int]("value")
  }

  test("session without a keyspace has no logged keyspace") {
    session.loggedKeyspace.toTry.get shouldEqual None
  }

  test("connect(keyspace) logs the keyspace and resolves unqualified tables") {
    val program = cluster.connect(keyspace).use { session =>
      for {
        loggedKeyspace <- session.loggedKeyspace
        resultSet <- session.execute(s"SELECT value FROM $table WHERE id = 1")
      } yield (loggedKeyspace, resultSet.one().decode[String]("value"))
    }
    program.toTry.get shouldEqual ((Some(keyspace), "value"))
  }

  test("health check statement runs against the server") {
    CassandraHealthCheck.Statement.of[IO](session, ConsistencyLevel.ONE).flatten.toTry.get
  }
}

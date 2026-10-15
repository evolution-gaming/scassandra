package com.evolution.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.datastax.oss.driver.api.core.`type`.codec.CodecNotFoundException
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile
import com.datastax.oss.driver.api.core.servererrors.{InvalidQueryException, SyntaxError}
import com.datastax.oss.driver.api.core.{CqlIdentifier, DefaultConsistencyLevel}
import com.evolution.scassandra.syntax.*
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.jdk.OptionConverters.*

class SessionSpec extends AnyFunSuite with CassandraSuite with Matchers with EitherValues {

  private lazy val table = "session_spec"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val program = for {
      _ <- session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.$table (id INT PRIMARY KEY, value TEXT)")
      _ <- session.execute(s"INSERT INTO $keyspace.$table (id, value) VALUES (1, 'value')")
    } yield ()
    program.unsafeRunSync()
  }

  private def failure(io: IO[?]): Throwable = io.attempt.unsafeRunSync().left.value

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
    val row = session.execute(s"SELECT value FROM $keyspace.$table WHERE id = 1").unsafeRunSync().one()
    an[IllegalArgumentException] should be thrownBy row.decode[String]("missing")
    an[IllegalArgumentException] should be thrownBy row.decode[Option[String]]("missing")
  }

  test("decoding a column as a wrong type fails with CodecNotFoundException") {
    val row = session.execute(s"SELECT value FROM $keyspace.$table WHERE id = 1").unsafeRunSync().one()
    a[CodecNotFoundException] should be thrownBy row.decode[Int]("value")
  }

  test("statements are immutable, encoding returns a new statement") {
    val prepared = session.prepare(s"SELECT value FROM $keyspace.$table WHERE id = ?").unsafeRunSync()
    val bound = prepared.bind()
    val encoded = bound.encode("id", 1)

    bound.isSet("id") shouldEqual false
    encoded.isSet("id") shouldEqual true
    session.execute(encoded).unsafeRunSync().one().decode[String]("value") shouldEqual "value"
  }

  test("session without a keyspace has no keyspace") {
    session.keyspace shouldEqual None
  }

  test("session keyspace resolves unqualified tables") {
    val result = withSession(s"basic.session-keyspace = $keyspace") { session =>
      for {
        resultSet <- session.execute(s"SELECT value FROM $table WHERE id = 1")
      } yield (session.keyspace, resultSet.one().decode[String]("value"))
    }
    result shouldEqual ((Some(CqlIdentifier.fromCql(keyspace)), "value"))
  }

  test("metadata contains the keyspace and the table") {
    val metadata = session.metadata.unsafeRunSync()
    val keyspaceMetadata = metadata.getKeyspace(keyspace).toScala

    keyspaceMetadata.flatMap(_.getTable(table).toScala).map(_.getName.asInternal()) shouldEqual Some(table)
  }

  test("health check statement runs against the server") {
    CassandraHealthCheck.Statement.of[IO](session, DefaultConsistencyLevel.ONE).flatten.unsafeRunSync()
  }

  test("NextHostRetryPolicy is created by the driver from the configuration") {
    val retryPolicy =
      """
        |advanced.retry-policy {
        |  class = com.evolution.scassandra.NextHostRetryPolicy
        |  max-retries = 1
        |}
        |""".stripMargin
    val result = withSession(retryPolicy) { session =>
      for {
        resultSet <- session.execute(s"SELECT value FROM $keyspace.$table WHERE id = 1")
      } yield {
        val policy = session.unwrap.getContext.getRetryPolicy(DriverExecutionProfile.DEFAULT_NAME)
        (policy.getClass, resultSet.one().decode[String]("value"))
      }
    }
    result shouldEqual ((classOf[NextHostRetryPolicy], "value"))
  }
}

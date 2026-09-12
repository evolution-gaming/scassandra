package com.evolutiongaming.scassandra

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref, Resource}
import cats.syntax.all.*
import com.datastax.driver.core.{
  BoundStatement,
  CodecRegistry,
  ColumnDefinitionsMock,
  ConsistencyLevel,
  PreparedIdMock,
  PreparedStatement,
  ResultSet,
  Statement,
}
import com.evolutiongaming.catshelper.{Log, LogOf}
import org.scalatest.Succeeded
import org.scalatest.funsuite.AsyncFunSuite

import scala.concurrent.duration.*
import scala.util.control.NoStackTrace

class CassandraHealthCheckSpec extends AsyncFunSuite {

  test("CassandraHealthCheck#of(statement)") {

    val expectedError = new RuntimeException with NoStackTrace

    val healthCheck = CassandraHealthCheck.of[IO](
      initial = 0.seconds,
      interval = 1.second,
      statement = Resource.eval(expectedError.raiseError[IO, Unit].pure[IO]),
      log = Log.empty[IO],
    )

    val actualError = healthCheck.use(_.error.untilDefinedM)

    val program = actualError.map { actualError =>
      assert(actualError == expectedError)
    }

    program.timeout(10.seconds).as(Succeeded).unsafeToFuture()
  }

  test("CassandraHealthCheck#of(statement) reports no error when the statement succeeds") {

    val healthCheck = CassandraHealthCheck.of[IO](
      initial = 0.seconds,
      interval = 10.millis,
      statement = Resource.eval(IO.unit.pure[IO]),
      log = Log.empty[IO],
    )

    val program = healthCheck.use { healthCheck =>
      for {
        _ <- IO.sleep(100.millis)
        error <- healthCheck.error
      } yield assert(error.isEmpty)
    }

    program.timeout(10.seconds).as(Succeeded).unsafeToFuture()
  }

  test("CassandraHealthCheck#of(statement) reports no error before the first check") {

    val expectedError = new RuntimeException with NoStackTrace

    val healthCheck = CassandraHealthCheck.of[IO](
      initial = 1.hour,
      interval = 1.hour,
      statement = Resource.eval(expectedError.raiseError[IO, Unit].pure[IO]),
      log = Log.empty[IO],
    )

    val program = healthCheck.use(_.error).map { error =>
      assert(error.isEmpty)
    }

    program.timeout(10.seconds).as(Succeeded).unsafeToFuture()
  }

  test("CassandraHealthCheck#of(statement) recovers once the statement succeeds again") {

    val expectedError = new RuntimeException with NoStackTrace

    val program = for {
      healthy <- Ref[IO].of(false)
      statement = healthy.get.flatMap { healthy =>
        if (healthy) IO.unit else expectedError.raiseError[IO, Unit]
      }
      healthCheck = CassandraHealthCheck.of[IO](
        initial = 0.seconds,
        interval = 10.millis,
        statement = Resource.eval(statement.pure[IO]),
        log = Log.empty[IO],
      )
      result <- healthCheck.use { healthCheck =>
        for {
          error <- healthCheck.error.untilDefinedM
          _ <- healthy.set(true)
          recovered <- healthCheck.error.iterateUntil(_.isEmpty)
        } yield (error, recovered)
      }
    } yield {
      assert(result == ((expectedError, None)))
    }

    program.timeout(10.seconds).as(Succeeded).unsafeToFuture()
  }

  test("CassandraHealthCheck.Statement#of prepares system.local query and applies consistency level") {

    val program = for {
      session <- IO(new SessionMock)
      statement <- CassandraHealthCheck.Statement.of[IO](session, ConsistencyLevel.LOCAL_QUORUM)
      _ <- statement
      executed <- session.executed.get
    } yield {
      assert(session.prepared == List("SELECT now() FROM system.local"))
      assert(executed.map(_.getConsistencyLevel) == List(ConsistencyLevel.LOCAL_QUORUM))
      assert(executed.map(_.asInstanceOf[BoundStatement].preparedStatement()) ==
        List(session.preparedStatement))
    }

    program.timeout(10.seconds).as(Succeeded).unsafeToFuture()
  }

  test("CassandraHealthCheck#of(session) prepares the statement on acquisition and starts healthy") {

    implicit val logOf: LogOf[IO] = LogOf.empty[IO]

    val program = for {
      session <- IO(new SessionMock)
      healthCheck =
        CassandraHealthCheck.of[IO](Resource.pure[IO, CassandraSession[IO]](session), ConsistencyLevel.ONE)
      error <- healthCheck.use(_.error)
      executed <- session.executed.get
    } yield {
      assert(session.prepared == List("SELECT now() FROM system.local"))
      assert(error.isEmpty)
      assert(executed.isEmpty)
    }

    program.timeout(10.seconds).as(Succeeded).unsafeToFuture()
  }

  private class SessionMock extends CassandraSessionMock {

    var prepared: List[String] = Nil

    val executed: Ref[IO, List[Statement]] = Ref.unsafe[IO, List[Statement]](Nil)

    lazy val preparedStatement: PreparedStatement = ProxyMock[PreparedStatement] {
      case ("getVariables", Nil) => ColumnDefinitionsMock.empty
      case ("getPreparedId", Nil) => PreparedIdMock.empty
      case ("getConsistencyLevel", Nil) => null
      case ("getSerialConsistencyLevel", Nil) => null
      case ("isTracing", Nil) => Boolean.box(false)
      case ("getRetryPolicy", Nil) => null
      case ("getOutgoingPayload", Nil) => null
      case ("getIncomingPayload", Nil) => null
      case ("getCodecRegistry", Nil) => CodecRegistry.DEFAULT_INSTANCE
      case ("isIdempotent", Nil) => null
      case ("bind", Nil) => new BoundStatement(preparedStatement)
    }

    override def prepare(query: String): IO[PreparedStatement] = IO {
      prepared = prepared :+ query
      preparedStatement
    }

    override def execute(statement: Statement): IO[ResultSet] = {
      executed.update(_ :+ statement).as(ResultSetMock())
    }
  }
}

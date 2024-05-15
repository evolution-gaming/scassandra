package com.evolutiongaming.scassandra

import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import com.evolutiongaming.catshelper.Log
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
      log = Log.empty[IO]
    )

    val actualError = healthCheck.use(_.error.untilDefinedM)

    val program = actualError.map { actualError =>
      assert(actualError == expectedError)
    }

    program.timeout(10.seconds).as(Succeeded).unsafeToFuture()
  }

}

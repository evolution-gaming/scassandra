package com.evolution.scassandra

import cats.effect.testkit.TestControl
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref, Resource}
import cats.syntax.all.*
import com.evolutiongaming.catshelper.Log
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*
import scala.util.control.NoStackTrace

class CassandraHealthCheckSpec extends AnyFunSuite with Matchers {

  private val unhealthy = new RuntimeException("unhealthy") with NoStackTrace

  private def healthCheck(statement: IO[Unit]): Resource[IO, CassandraHealthCheck[IO]] = {
    CassandraHealthCheck.make[IO](
      initial = 10.seconds,
      interval = 1.second,
      statement = Resource.pure[IO, IO[Unit]](statement),
      log = Log.empty[IO],
    )
  }

  // runs on a simulated clock, so the delays take no real time. The checks run at whole seconds, while the
  // test reads at half seconds, as the tasks due at the same time run in a random order
  private def run[A](program: IO[A]): A = TestControl.executeEmbed(program).unsafeRunSync()

  test("no error before the first check") {
    val error = healthCheck(unhealthy.raiseError[IO, Unit]).use { healthCheck =>
      IO.sleep(9.seconds) *> healthCheck.error
    }

    run(error) shouldEqual None
  }

  test("failed check is reported") {
    val error = healthCheck(unhealthy.raiseError[IO, Unit]).use { healthCheck =>
      IO.sleep(10.seconds + 500.millis) *> healthCheck.error
    }

    run(error) shouldEqual Some(unhealthy)
  }

  test("checks are repeated with the interval and report no error while they succeed") {
    val program = for {
      checks <- Ref[IO].of(0)
      error <- healthCheck(checks.update(_ + 1)).use { healthCheck =>
        IO.sleep(14.seconds + 500.millis) *> healthCheck.error
      }
      checked <- checks.get
    } yield (error, checked)

    run(program) shouldEqual ((None, 5))
  }

  test("error is cleared once a check succeeds again") {
    val program = for {
      healthy <- Ref[IO].of(false)
      statement = healthy.get.flatMap { healthy => unhealthy.raiseError[IO, Unit].unlessA(healthy) }
      errors <- healthCheck(statement).use { healthCheck =>
        for {
          _ <- IO.sleep(10.seconds + 500.millis)
          failed <- healthCheck.error
          _ <- healthy.set(true)
          _ <- IO.sleep(1.second)
          recovered <- healthCheck.error
        } yield (failed, recovered)
      }
    } yield errors

    run(program) shouldEqual ((Some(unhealthy), None))
  }
}

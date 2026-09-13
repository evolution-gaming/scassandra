package com.evolutiongaming.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.evolutiongaming.scassandra.util.FromCompletionStage
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import scala.util.control.NoStackTrace

class FromCompletionStageSpec extends AnyFunSuite with Matchers {

  test("completed stage") {
    FromCompletionStage[IO, Int](CompletableFuture.completedFuture(1)).unsafeRunSync() shouldEqual 1
  }

  test("failed stage") {
    val error = new RuntimeException("boom") with NoStackTrace
    FromCompletionStage[IO, Int](CompletableFuture.failedFuture(error)).attempt.unsafeRunSync() shouldEqual
      Left(error)
  }

  test("stage is created lazily and on every run") {
    val created = new AtomicInteger(0)
    val io = FromCompletionStage[IO, Int] { CompletableFuture.completedFuture(created.incrementAndGet()) }
    created.get() shouldEqual 0
    io.unsafeRunSync() shouldEqual 1
    io.unsafeRunSync() shouldEqual 2
  }

  test("stage completed later") {
    val future = new CompletableFuture[Int]()
    val program = for {
      fiber <- FromCompletionStage[IO, Int](future).start
      _ <- IO(future.complete(42))
      a <- fiber.joinWithNever
    } yield a
    program.unsafeRunSync() shouldEqual 42
  }

  test("stage is cancelled when F is cancelled") {
    val future = new CompletableFuture[Int]()
    val program = for {
      fiber <- FromCompletionStage[IO, Int](future).start
      _ <- IO.cede.untilM_(IO(future.getNumberOfDependents > 0))
      _ <- fiber.cancel
    } yield ()
    program.unsafeRunSync()
    future.isCancelled shouldEqual true
  }
}

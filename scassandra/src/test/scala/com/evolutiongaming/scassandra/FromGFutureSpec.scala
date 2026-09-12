package com.evolutiongaming.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.evolutiongaming.scassandra.util.FromGFuture
import com.google.common.util.concurrent.{AbstractFuture, Futures, SettableFuture}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CountDownLatch, Executor}
import scala.annotation.nowarn
import scala.util.control.NoStackTrace

class FromGFutureSpec extends AnyFunSuite with Matchers {

  test("completed future") {
    FromGFuture[IO].apply(Futures.immediateFuture(1)).unsafeRunSync() shouldEqual 1
  }

  test("failed future") {
    val error = new RuntimeException("boom") with NoStackTrace
    FromGFuture[IO].apply(Futures.immediateFailedFuture[Int](error)).attempt.unsafeRunSync() shouldEqual
      Left(error)
  }

  test("future is created lazily and on every run") {
    val created = new AtomicInteger(0)
    val io = FromGFuture[IO].apply {
      Futures.immediateFuture(created.incrementAndGet())
    }
    created.get() shouldEqual 0
    io.unsafeRunSync() shouldEqual 1
    io.unsafeRunSync() shouldEqual 2
  }

  test("future completed later") {
    val future = SettableFuture.create[Int]()
    val program = for {
      fiber <- FromGFuture[IO].apply(future).start
      _ <- IO(future.set(42))
      a <- fiber.joinWithNever
    } yield a
    program.unsafeRunSync() shouldEqual 42
  }

  test("future is cancelled when F is cancelled") {
    val future = new ObservableFuture
    val program = for {
      fiber <- FromGFuture[IO].apply(future).start
      _ <- IO.interruptible(future.listenerAdded.await())
      _ <- fiber.cancel
    } yield ()
    program.unsafeRunSync()
    future.isCancelled shouldEqual true
  }

  test("fromExecutor runs the callback on the provided executor") {
    val runs = new AtomicInteger(0)
    val executor: Executor = runnable => {
      runs.incrementAndGet()
      runnable.run()
    }
    FromGFuture.fromExecutor[IO](executor).apply(Futures.immediateFuture(1)).unsafeRunSync() shouldEqual 1
    runs.get() shouldEqual 1
  }

  private class ObservableFuture extends AbstractFuture[Int] {

    val listenerAdded = new CountDownLatch(1)

    override def addListener(listener: Runnable, executor: Executor): Unit = {
      super.addListener(listener, executor)
      listenerAdded.countDown()
    }
  }

  test("deprecated lift") {
    implicit val executor: Executor = _.run()
    (FromGFuture.lift[IO]: @nowarn(
      "cat=deprecation",
    )).apply(Futures.immediateFuture(1)).unsafeRunSync() shouldEqual 1
  }
}

package com.evolutiongaming.scassandra

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.evolutiongaming.scassandra.util.FromGFuture
import org.scalatest.Succeeded

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import cats.effect.Temporal

object IOSuite {
  val Timeout: FiniteDuration = 10.seconds

  implicit val executor: ExecutionContextExecutor = ExecutionContext.global
  implicit val fromGFutureIO: FromGFuture[IO] = FromGFuture.lift[IO]

  def runIO[A](io: IO[A], timeout: FiniteDuration = Timeout): Future[Succeeded.type] = {
    io.timeout(timeout).as(Succeeded).unsafeToFuture()
  }

  implicit class IOOps[A](val self: IO[A]) extends AnyVal {
    def run(timeout: FiniteDuration = Timeout): Future[Succeeded.type] = runIO(self, timeout)
  }
}

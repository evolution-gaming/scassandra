package com.evolutiongaming.scassandra.util

import cats.effect.{Async, Sync}
import cats.implicits._
import com.evolutiongaming.concurrent.ExecutionContextExecutorServiceFactory
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext

trait FromGFuture[F[_]] {

  def apply[A](future: => ListenableFuture[A]): F[A]
}

object FromGFuture {

  def apply[F[_]](implicit F: FromGFuture[F]): FromGFuture[F] = F

  @deprecated("use lift1", "3.4.0")
  def lift[F[_]: Async](implicit executor: Executor): FromGFuture[F] = fromExecutor(executor)

  implicit def lift1[F[_]: Async](implicit executor: ExecutionContext): FromGFuture[F] = {
    val executor1 = ExecutionContextExecutorServiceFactory(executor)
    fromExecutor(executor1)
  }

  def fromExecutor[F[_]: Async](executor: Executor): FromGFuture[F] = {
    class FromExecutor

    new FromExecutor with FromGFuture[F] {

      def apply[A](future: => ListenableFuture[A]) = {
        for {
          future <- Sync[F].delay { future }
          result <- Async[F].async[A] { callback =>
            val futureCallback = new FutureCallback[A] {
              def onSuccess(a: A) = callback(a.asRight)
              def onFailure(e: Throwable) = callback(e.asLeft)
            }
            Futures.addCallback(future, futureCallback, executor)
          }
        } yield result
      }
    }
  }
}

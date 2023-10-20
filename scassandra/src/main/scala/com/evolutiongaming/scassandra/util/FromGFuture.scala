package com.evolutiongaming.scassandra.util

import cats.effect.{Async, Sync}
import cats.implicits._
import com.evolutiongaming.concurrent.ExecutionContextExecutorServiceFactory
import com.google.common.util.concurrent.{
  FutureCallback,
  Futures,
  ListenableFuture
}

import java.util.concurrent.Executor

/** Converts any `ListenableFuture[A]` from Google Guava into `F[A]` */
trait FromGFuture[F[_]] {

  /** Suspends executuon of `future` in `F[_]`.
    *
    * I.e. there is no need to call `Sync[F].delay` on the argument first and
    * returned `F[A]` value could be reused as many times as needed.
    */
  def apply[A](future: => ListenableFuture[A]): F[A]
}

object FromGFuture {

  def apply[F[_]](implicit F: FromGFuture[F]): FromGFuture[F] = F

  @deprecated("use lift1", "4.1.0")
  def lift[F[_]: Async](implicit executor: Executor): FromGFuture[F] = fromExecutor(executor)

  implicit def lift1[F[_]: Async]: FromGFuture[F] = {
    class Lift1
    new Lift1 with FromGFuture[F] {

      def apply[A](future: => ListenableFuture[A]) = {
        for {
          executor    <- Async[F].executionContext
          fromGFuture  = fromExecutor(ExecutionContextExecutorServiceFactory(executor))
          result      <- fromGFuture { future }
        } yield result
      }
    }
  }

  def fromExecutor[F[_]: Async](executor: Executor): FromGFuture[F] = {
    class FromExecutor

    new FromExecutor with FromGFuture[F] {

      def apply[A](future: => ListenableFuture[A]) = {
        for {
          future <- Sync[F].delay { future }
          result <- Async[F].async_[A] { callback =>
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

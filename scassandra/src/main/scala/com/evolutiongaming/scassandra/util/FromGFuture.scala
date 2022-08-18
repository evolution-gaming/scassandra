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

trait FromGFuture[F[_]] {

  def apply[A](future: => ListenableFuture[A]): F[A]
}

object FromGFuture {

  def apply[F[_]](implicit F: FromGFuture[F]): FromGFuture[F] = F

  @deprecated("use FromGFuture.lift(implicit F: Async[F]) instead", "4.0.1")
  def lift[F[_]: Async](implicit executor: Executor): FromGFuture[F] = {

    new FromGFuture[F] {

      def apply[A](future: => ListenableFuture[A]) = {
        for {
          ec <- Async[F].executionContext
          executor <- ExecutionContextExecutorServiceFactory(ec).pure[F]
          future <- Sync[F].delay {
            future
          }
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

  implicit def liftFromAsync[F[_]: Async]: FromGFuture[F] = {

    new FromGFuture[F] {

      def apply[A](future: => ListenableFuture[A]) = {
        for {
          ec <- Async[F].executionContext
          executor <- ExecutionContextExecutorServiceFactory(ec).pure[F]
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

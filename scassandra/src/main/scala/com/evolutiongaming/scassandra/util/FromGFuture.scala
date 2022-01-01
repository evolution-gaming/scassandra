package com.evolutiongaming.scassandra.util

import java.util.concurrent.Executor

import cats.effect.{Async, Sync}
import cats.implicits._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

trait FromGFuture[F[_]] {

  def apply[A](future: => ListenableFuture[A]): F[A]
}

object FromGFuture {

  def apply[F[_]](implicit F: FromGFuture[F]): FromGFuture[F] = F


  implicit def lift[F[_] : Async](implicit executor: Executor): FromGFuture[F] = {

    new FromGFuture[F] {

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
package com.evolution.scassandra.util

import cats.effect.{Async, Sync}
import cats.syntax.all.*

import java.util.concurrent.{CompletionException, CompletionStage}

private[scassandra] object FromCompletionStage {

  /**
   * Suspends the creation of `completionStage` and waits for its completion.
   *
   * Cancellation of the returned effect cancels the underlying future, which aborts the
   * driver request.
   */
  def apply[F[_]: Async, A](completionStage: => CompletionStage[A]): F[A] = {
    // unlike `Async#fromCompletionStage`, the future is created and its cancellation is registered in one
    // uncancelable step, so a request, once sent, is always cancelled along with the effect
    Async[F].async[A] { callback =>
      Sync[F].delay {
        val future = completionStage.toCompletableFuture
        future.handle[Unit] { (value: A, error: Throwable) =>
          callback(Option(error).map(unwrap).toLeft(value))
        }
        Sync[F].delay { future.cancel(false) }.void.some
      }
    }
  }

  private def unwrap(error: Throwable): Throwable = {
    error match {
      case error: CompletionException => Option(error.getCause).getOrElse(error)
      case error => error
    }
  }
}

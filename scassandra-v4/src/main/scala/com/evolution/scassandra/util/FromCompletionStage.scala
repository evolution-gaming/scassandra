package com.evolution.scassandra.util

import cats.effect.{Async, Sync}

import java.util.concurrent.CompletionStage

private[scassandra] object FromCompletionStage {

  /**
   * Suspends the creation of `completionStage` and waits for its completion.
   *
   * Cancellation of the returned effect cancels the underlying future, which aborts the
   * driver request.
   */
  def apply[F[_]: Async, A](completionStage: => CompletionStage[A]): F[A] = {
    Async[F].fromCompletionStage(Sync[F].delay(completionStage))
  }
}

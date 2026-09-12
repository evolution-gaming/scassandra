package com.evolutiongaming.scassandra.util

import cats.effect.{Async, Sync}

import java.util.concurrent.CompletionStage

/**
 * Converts `CompletionStage[A]` into `F[A]`, the stage is created on every run of the
 * returned `F[A]` and cancelled when `F[A]` is cancelled.
 */
object FromCompletionStage {

  def apply[F[_]: Async, A](stage: => CompletionStage[A]): F[A] = {
    Async[F].fromCompletionStage(Sync[F].delay(stage))
  }
}

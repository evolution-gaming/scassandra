package com.evolution.scassandra4.util

import cats.effect.{Async, Sync}

import java.util.concurrent.CompletionStage

// TODO: WIP remove since Async[F] is enough, no need for a separate typeclass

/** Converts any `CompletionStage[A]` returned by the Java driver into `F[A]`.
 *
 * The driver 4 counterpart of `FromGFuture` from the driver 3 based module:
 * driver 4 returns `CompletionStage` instead of Guava's `ListenableFuture`.
 */
trait FromCompletionStage[F[_]] {

  /** Suspends execution of `stage` in `F[_]`.
   *
   * I.e. there is no need to call `Sync[F].delay` on the argument first and
   * the returned `F[A]` value could be reused as many times as needed.
   *
   * Example:
   * {{{
   * FromCompletionStage[F].apply { session.executeAsync("SELECT name FROM users") }
   * }}}
   */
  def apply[A](stage: => CompletionStage[A]): F[A]
}

object FromCompletionStage {

  def apply[F[_]](implicit F: FromCompletionStage[F]): FromCompletionStage[F] = F

  implicit def lift[F[_] : Async]: FromCompletionStage[F] = {
    new FromCompletionStage[F] {
      def apply[A](stage: => CompletionStage[A]) = {
        Async[F].fromCompletableFuture(Sync[F].delay(stage.toCompletableFuture))
      }
    }
  }
}

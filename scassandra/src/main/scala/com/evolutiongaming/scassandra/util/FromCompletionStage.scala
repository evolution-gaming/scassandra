package com.evolutiongaming.scassandra.util
import cats.effect.Async

import java.util.concurrent.CompletionStage

object FromCompletionStage {
  def apply[F[_]: Async, T](stage: => CompletionStage[T]): F[T] = {
    Async[F].async { callback =>
      Async[F].delay {
        val s = stage
        s.whenComplete { (res, err) =>
          if (res != null) callback(Right(res))
          else if (err != null) callback(Left(err))
          else callback(Left(new RuntimeException("Operation failed with no error")))
        }
        None
      }
    }
  }
}

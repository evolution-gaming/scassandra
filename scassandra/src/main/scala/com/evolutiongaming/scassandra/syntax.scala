package com.evolutiongaming.scassandra

import com.datastax.driver.core.{BoundStatement, Row}
import com.evolutiongaming.concurrent.CurrentThreadExecutionContext
import com.google.common.util.concurrent.ListenableFuture

import scala.concurrent.{ExecutionException, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Try}

object syntax {

  implicit class ListenableFutureOps[T](val self: ListenableFuture[T]) extends AnyVal {

    // TODO
    def await(): Try[T] = {
      val safe = Try(self.get())
      safe.recoverWith { case failure: ExecutionException => Failure(failure.getCause) }
    }

    // TODO
    def asScala(): Future[T] = {
      if (self.isDone) {
        Future.fromTry(await())
      } else {
        val promise = Promise[T]
        val runnable = new Runnable {
          def run() = promise.complete(await())
        }
        self.addListener(runnable, CurrentThreadExecutionContext)
        promise.future
      }
    }
  }

  implicit def encodeOps(a: BoundStatement) = new Encode.Ops.BoundStatementOps(a)

  implicit def decodeOps(a: Row) = new Decode.Ops.RowOps(a)

  implicit def encodeRowOps(a: BoundStatement) = new EncodeRow.Ops.BoundStatementOps(a)

  implicit def decodeRowOps(a: Row) = new DecodeRow.Ops.RowOps(a)

  implicit def toCqlOps[A](a: A) = new ToCql.Ops.IdOps(a)
}

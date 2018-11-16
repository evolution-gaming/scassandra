package com.evolutiongaming.scassandra

import com.datastax.driver.core.{GettableByIndexData, GettableByNameData, SettableData}
import com.evolutiongaming.concurrent.CurrentThreadExecutionContext
import com.google.common.util.concurrent.ListenableFuture

import scala.concurrent.{ExecutionException, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Try}

object syntax {

  implicit class ListenableFutureOps[A](val self: ListenableFuture[A]) extends AnyVal {

    def await(): Try[A] = {
      val safe = Try(self.get())
      safe.recoverWith { case failure: ExecutionException => Failure(failure.getCause) }
    }

    def asScala(): Future[A] = {
      if (self.isDone) {
        Future.fromTry(await())
      } else {
        val promise = Promise[A]
        val runnable = new Runnable {
          def run() = promise.complete(await())
        }
        self.addListener(runnable, CurrentThreadExecutionContext)
        promise.future
      }
    }
  }

  implicit class SettableDataOps[A <: SettableData[A]](val self: A) extends AnyVal {

    def encode[B](name: String, value: B)(implicit encode: EncodeByName[B]): A = {
      encode(self, name, value)
    }

    def encodeAt[B](idx: Int, value: B)(implicit encode: EncodeByIdx[B]): A = {
      encode(self, idx, value)
    }

    def encode[B](value: B)(implicit encode: EncodeRow[B]): A = {
      encode(self, value)
    }
  }


  implicit class GettableByNameDataOps(val self: GettableByNameData) extends AnyVal {

    def decode[A](name: String)(implicit decode: DecodeByName[A]): A = {
      decode(self, name)
    }

    def decode[A](implicit decode: DecodeRow[A]): A = {
      decode(self)
    }
  }


  implicit class GettableByIdxDataOps(val self: GettableByIndexData) extends AnyVal {

    def decodeAt[A](idx: Int)(implicit decode: DecodeByIdx[A]): A = {
      decode(self, idx)
    }
  }


  implicit def toCqlOps[A](a: A) = new ToCql.Ops.IdOps(a)
}

package com.evolutiongaming.scassandra

import com.datastax.driver.core.{GettableByIndexData, GettableByNameData, SettableData, Statement}
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions

object syntax {

  implicit class ListenableFutureOps[A](val self: ListenableFuture[A]) extends AnyVal {

    def asScala(): Future[A] = {
      val promise = Promise[A]
      val callback = new FutureCallback[A] {
        def onSuccess(result: A) = promise.success(result)
        def onFailure(cause: Throwable) = promise.failure(cause)
      }
      Futures.addCallback(self, callback)
      promise.future
    }
  }

  implicit class ScassandraSettableDataOps[A <: SettableData[A]](val self: A) extends AnyVal {

    def encode[B](name: String, value: B)(implicit encode: EncodeByName[B]): A = {
      encode(self, name, value)
    }

    def encode[B](value: B)(implicit encode: EncodeRow[B]): A = {
      encode(self, value)
    }

    def encodeAt[B](idx: Int, value: B)(implicit encode: EncodeByIdx[B]): A = {
      encode(self, idx, value)
    }

    def encodeSome[B](name: String, value: Option[B])(implicit encode: EncodeByName[B]): A = {
      value.fold(self)(encode(self, name, _))
    }

    def encodeSome[B](value: Option[B])(implicit encode: EncodeRow[B]): A = {
      value.fold(self)(encode(self, _))
    }
  }


  implicit class ScassandraGettableByNameDataOps(val self: GettableByNameData) extends AnyVal {

    def decode[A](name: String)(implicit decode: DecodeByName[A]): A = {
      decode(self, name)
    }

    def decode[A](implicit decode: DecodeRow[A]): A = {
      decode(self)
    }
  }


  implicit class ScassandraGettableByIdxDataOps(val self: GettableByIndexData) extends AnyVal {

    def decodeAt[A](idx: Int)(implicit decode: DecodeByIdx[A]): A = {
      decode(self, idx)
    }
  }


  implicit def toCqlOps[A](a: A) = new ToCql.Ops.IdOps(a)


  implicit class ScassandraStatementOps(val self: Statement) extends AnyVal {

    def trace(enable: Boolean): Statement = {
      if (enable) self.enableTracing()
      else self.disableTracing()
    }
  }
}

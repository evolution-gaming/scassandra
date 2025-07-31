package com.evolutiongaming.scassandra

import cats.effect.implicits.*
import cats.effect.{Async, Sync}
import cats.syntax.all.*
import com.datastax.driver.core.*
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, Bindable, Row, Statement}
import com.datastax.oss.driver.api.core.data.{GettableByIndex, GettableByName}
import com.evolutiongaming.scassandra.util.FromGFuture
import com.evolutiongaming.sstream.FoldWhile.*
import com.evolutiongaming.sstream.Stream

object syntax {
  implicit class ResultSetOps(private val self: AsyncResultSet) extends AnyVal {
    def stream[F[_]: Async]: Stream[F, Row] =
      StreamingCassandraSession.toStream[F](self)
  }

  implicit class ScassandraSettableDataOps[A <: Bindable[A]](private val self: A) extends AnyVal {

    def encode[B](name: String, value: B)(implicit encode: EncodeByName[B]): A = {
      encode.bindToData(self, name, value)
    }

    def encode[B](value: B)(implicit encode: EncodeRow[B]): A = {
      encode(self, value)
    }

    def encodeAt[B](idx: Int, value: B)(implicit encode: EncodeByIdx[B]): A = {
      encode(self, idx, value)
    }

    def encodeSome[B](name: String, value: Option[B])(implicit
        encode: EncodeByName[B]
    ): A = {
      value.fold(self)(encode.bindToData(self, name, _))
    }

    def encodeSome[B](value: Option[B])(implicit encode: EncodeRow[B]): A = {
      value.fold(self)(encode(self, _))
    }
  }

  implicit class ScassandraGettableByNameDataOps(private val self: GettableByName) extends AnyVal {
    def decode[A](name: String)(implicit decode: DecodeByName[A]): A = {
      decode(self, name)
    }

    def decode[A](implicit decode: DecodeRow[A]): A = {
      decode(self)
    }
  }

  implicit class ScassandraGettableByIdxDataOps(private val self: GettableByIndex) extends AnyVal {
    def decodeAt[A](idx: Int)(implicit decode: DecodeByIdx[A]): A = {
      decode(self, idx)
    }
  }

  implicit def toCqlOps[A](a: A): ToCql.implicits.IdOpsToCql[A] = new ToCql.implicits.IdOpsToCql(a)

  implicit class ScassandraStatementOps[T <: Statement[T]](private val self: Statement[T]) extends AnyVal {
    def trace(enable: Boolean): Statement[T] =
      self.setTracing(enable)
  }

  implicit class ScassandraUpdateSyntax[D <: Bindable[D]](private val data: D) extends AnyVal {
    def update[A](value: A)(implicit update: UpdateRow[A]): D = {
      update(data, value)
    }

    def update[A](name: String, value: A)(implicit update: UpdateByName[A]): D = {
      update(data, name, value)
    }

    def updateAt[A](idx: Int, value: A)(implicit update: UpdateByIdx[A]): D = {
      update(data, idx, value)
    }
  }
}

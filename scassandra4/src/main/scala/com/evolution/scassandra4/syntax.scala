package com.evolution.scassandra4

import cats.effect.Async
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, Row, Statement}
import com.datastax.oss.driver.api.core.data.{GettableByIndex, GettableByName, SettableByName}
import com.evolution.scassandra4.util.FromCompletionStage
import com.evolutiongaming.sstream.Stream

object syntax {

  implicit class ScassandraAsyncResultSetOps(val self: AsyncResultSet) extends AnyVal {

    def stream[F[_]: Async: FromCompletionStage]: Stream[F, Row] = {
      StreamingCassandraSession.toStream(self)
    }
  }

  implicit class ScassandraSettableByNameOps[A <: SettableByName[A]](val self: A)
      extends AnyVal {

    def encode[B](name: String, value: B)(implicit
        encode: EncodeByName[B]
    ): A = {
      encode(self, name, value)
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
      value.fold(self)(encode(self, name, _))
    }

    def encodeSome[B](value: Option[B])(implicit encode: EncodeRow[B]): A = {
      value.fold(self)(encode(self, _))
    }
  }

  implicit class ScassandraGettableByNameOps(val self: GettableByName)
      extends AnyVal {

    def decode[A](name: String)(implicit decode: DecodeByName[A]): A = {
      decode(self, name)
    }

    def decode[A](implicit decode: DecodeRow[A]): A = {
      decode(self)
    }
  }

  implicit class ScassandraGettableByIdxOps(val self: GettableByIndex)
      extends AnyVal {

    def decodeAt[A](idx: Int)(implicit decode: DecodeByIdx[A]): A = {
      decode(self, idx)
    }
  }

  implicit def toCqlOps[A](a: A): ToCql.implicits.IdOpsToCql[A] = new ToCql.implicits.IdOpsToCql(a)

  implicit class ScassandraStatementOps[A <: Statement[A]](val self: A) extends AnyVal {

    def trace(enable: Boolean): A = self.setTracing(enable)
  }

  implicit class ScassandraUpdateSyntax[D <: GettableByName & SettableByName[D]](val data: D) extends AnyVal {

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

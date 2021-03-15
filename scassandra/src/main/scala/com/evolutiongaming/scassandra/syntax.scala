package com.evolutiongaming.scassandra

import cats.effect.implicits._
import cats.effect.{Concurrent, Sync}
import cats.syntax.all._
import com.datastax.driver.core._
import com.evolutiongaming.scassandra.util.FromGFuture
import com.evolutiongaming.sstream.FoldWhile._
import com.evolutiongaming.sstream.Stream

import scala.language.implicitConversions

object syntax {

  implicit class ResultSetOps(val self: ResultSet) extends AnyVal {

    def stream[F[_] : Concurrent : FromGFuture]: Stream[F, Row] = {
      val iterator = self.iterator()
      val fetch = FromGFuture[F].apply { self.fetchMoreResults() }.void
      val fetched = Sync[F].delay { self.isFullyFetched }
      val next = Sync[F].delay { List.fill(self.getAvailableWithoutFetching)(iterator.next()) }

      new Stream[F, Row] {

        def foldWhileM[L, R](l: L)(f: (L, Row) => F[Either[L, R]]) = {

          l.tailRecM[F, Either[L, R]] { l =>

            def apply(rows: List[Row]) = {
              for {
                result <- rows.foldWhileM(l)(f)
              } yield {
                result.asRight[L]
              }
            }

            def fetchAndApply(rows: List[Row]) = {
              for {
                fetching <- fetch.start
                result <- rows.foldWhileM(l)(f)
                result <- result match {
                  case l: Left[L, R]  => fetching.join as l.rightCast[Either[L, R]]
                  case r: Right[L, R] => r.leftCast[L].asRight[L].pure[F]
                }
              } yield result
            }

            for {
              fetched <- fetched
              rows <- next
              result <- if (fetched) apply(rows) else fetchAndApply(rows)
            } yield result
          }
        }
      }
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

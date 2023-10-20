package com.evolutiongaming.scassandra

import cats.Functor
import com.datastax.driver.core.GettableByNameData

/** Reconstructs `A` from a row received from Cassandra.
  *
  * Takes one or more fields from a row and converts them to `A`.
  *
  * In other words it could be treated as set of [[DecodeByName]]
  * and/or [[DecodeByIdx]] instances, where the names or the indices
  * of a required fields are already build-in.
  *
  * Example:
  * {{{
  * implicit val userId: DecodeRow[UserId] = DecodeRow("userId")
  *
  * session.execute("SELECT userId, amount FROM wallet").flatMap { resultSet =>
  *   val row = resultSet.one()
  *   val userId = row.decode[UserId]
  * }
  * }}}
  */
trait DecodeRow[A] {

  def apply(data: GettableByNameData): A
}

object DecodeRow {

  implicit val functorDecodeRow: Functor[DecodeRow] = new Functor[DecodeRow] {
    def map[A, B](fa: DecodeRow[A])(f: A => B) = fa.map(f)
  }


  def apply[A](implicit decode: DecodeRow[A]): DecodeRow[A] = decode

  def apply[A](name: String)(implicit decode: DecodeByName[A]): DecodeRow[A] = new DecodeRow[A] {
    def apply(data: GettableByNameData) = decode(data, name)
  }


  object Ops {

    implicit class GettableByNameDataOps(val self: GettableByNameData) extends AnyVal {

      def decode[A](implicit decode: DecodeRow[A]): A = decode(self)
    }
  }


  implicit class DecodeRowOps[A](val self: DecodeRow[A]) extends AnyVal {

    def map[B](f: A => B): DecodeRow[B] = new DecodeRow[B] {
      def apply(data: GettableByNameData) = f(self(data))
    }
  }
}

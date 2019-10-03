package com.evolutiongaming.scassandra

import cats.Functor
import com.datastax.driver.core.GettableByNameData


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

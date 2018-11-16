package com.evolutiongaming.scassandra

import com.datastax.driver.core.GettableByNameData


trait DecodeRow[A] extends { self =>

  def apply(data: GettableByNameData): A


  final def map[B](f: A => B): DecodeRow[B] = new DecodeRow[B] {
    def apply(data: GettableByNameData) = f(self(data))
  }
}

object DecodeRow {

  def apply[A](implicit decode: DecodeRow[A]): DecodeRow[A] = decode

  def apply[A](name: String)(implicit decode: DecodeByName[A]): DecodeRow[A] = new DecodeRow[A] {
    def apply(data: GettableByNameData) = decode(data, name)
  }


  object Ops {

    implicit class GettableByNameDataOps(val self: GettableByNameData) extends AnyVal {

      def decode[A](implicit decode: DecodeRow[A]): A = decode(self)
    }
  }
}

package com.evolutiongaming.scassandra

import com.datastax.driver.core.SettableData


trait EncodeRow[-A] { self =>

  def apply[B <: SettableData[B]](data: B, value: A): B


  final def imap[B](f: B => A): EncodeRow[B] = new EncodeRow[B] {
    def apply[C <: SettableData[C]](data: C, value: B) = self(data, f(value))
  }
}

object EncodeRow {

  def apply[A](implicit encode: EncodeRow[A]): EncodeRow[A] = encode

  def apply[A](name: String)(implicit encode: EncodeByName[A]): EncodeRow[A] = new EncodeRow[A] {
    def apply[B <: SettableData[B]](data: B, value: A) = encode(data, name, value)
  }

  object Ops {

    implicit class SettableDataOps[A <: SettableData[A]](val self: A) extends AnyVal {

      def encode[B](value: B)(implicit encode: EncodeRow[B]): A = encode(self, value)
    }
  }
}
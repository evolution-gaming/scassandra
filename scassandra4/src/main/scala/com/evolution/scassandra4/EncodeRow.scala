package com.evolution.scassandra4

import cats.Contravariant
import com.datastax.oss.driver.api.core.data.SettableByName


trait EncodeRow[-A] {

  def apply[B <: SettableByName[B]](data: B, value: A): B
}

object EncodeRow {

  implicit val contravariantEncodeRow: Contravariant[EncodeRow] = new Contravariant[EncodeRow] {
    def contramap[A, B](fa: EncodeRow[A])(f: B => A) = fa.contramap(f)
  }


  def apply[A](implicit encode: EncodeRow[A]): EncodeRow[A] = encode

  def apply[A](name: String)(implicit encode: EncodeByName[A]): EncodeRow[A] = new EncodeRow[A] {
    def apply[B <: SettableByName[B]](data: B, value: A) = encode(data, name, value)
  }


  object Ops {

    implicit class SettableByNameOps[A <: SettableByName[A]](val self: A) extends AnyVal {

      def encode[B](value: B)(implicit encode: EncodeRow[B]): A = encode(self, value)
    }
  }


  implicit class EncodeRowOps[A](val self: EncodeRow[A]) extends AnyVal {

    def contramap[B](f: B => A): EncodeRow[B] = new EncodeRow[B] {
      def apply[C <: SettableByName[C]](data: C, value: B) = self(data, f(value))
    }
  }
}

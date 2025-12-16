package com.evolutiongaming.scassandra

import cats.Contravariant

trait ToCql[-A] {
  def apply(a: A): String
}

object ToCql {
  implicit val contravariantToCql: Contravariant[ToCql] = new Contravariant[ToCql] {
    def contramap[A, B](fa: ToCql[A])(f: B => A): ToCql[B] = fa.contramap(f)
  }

  def apply[A](implicit toCql: ToCql[A]): ToCql[A] = toCql

  def apply[A: ToCql](a: A): String = ToCql[A].apply(a)

  implicit val strToCql: ToCql[String] = (a: String) => a

  object implicits {
    implicit class IdOpsToCql[A](private val self: A) extends AnyVal {
      def toCql(implicit toCql: ToCql[A]): String = ToCql(self)
    }
  }

  implicit class ToCqlOps[A](private val self: ToCql[A]) extends AnyVal {
    final def contramap[B](f: B => A): ToCql[B] = (a: B) => self(f(a))
  }
}
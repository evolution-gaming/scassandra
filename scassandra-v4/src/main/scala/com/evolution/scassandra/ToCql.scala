package com.evolution.scassandra

import cats.Contravariant

trait ToCql[-A] {

  def apply(value: A): String
}

object ToCql {

  implicit val contravariantToCql: Contravariant[ToCql] = new Contravariant[ToCql] {
    override def contramap[A, B](fa: ToCql[A])(f: B => A): ToCql[B] = fa.contramap(f)
  }

  def apply[A](
    implicit
    toCql: ToCql[A],
  ): ToCql[A] = toCql

  def apply[A: ToCql](value: A): String = ToCql[A].apply(value)

  implicit val strToCql: ToCql[String] = (value: String) => value

  implicit class ToCqlOps[A](val self: ToCql[A]) extends AnyVal {

    final def contramap[B](f: B => A): ToCql[B] = (value: B) => self(f(value))
  }
}

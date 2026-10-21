package com.evolution.scassandra

import cats.Contravariant
import com.datastax.oss.driver.api.core.data.SettableByName

/**
 * Stores `A` into one or more named columns of [[SettableByName]].
 *
 * Note, that the statements of the Java driver 4 are immutable, so the returned value
 * should be used, while `data` stays unchanged.
 */
trait EncodeRow[-A] {

  def apply[B <: SettableByName[B]](data: B, value: A): B
}

object EncodeRow {

  implicit val contravariantEncodeRow: Contravariant[EncodeRow] = new Contravariant[EncodeRow] {
    override def contramap[A, B](fa: EncodeRow[A])(f: B => A): EncodeRow[B] = fa.contramap(f)
  }

  def apply[A](
    implicit
    encode: EncodeRow[A],
  ): EncodeRow[A] = encode

  def apply[A](
    name: String,
  )(implicit
    encode: EncodeByName[A],
  ): EncodeRow[A] = new EncodeRow[A] {
    override def apply[B <: SettableByName[B]](data: B, value: A): B = encode(data, name, value)
  }

  implicit class EncodeRowOps[A](val self: EncodeRow[A]) extends AnyVal {

    def contramap[B](f: B => A): EncodeRow[B] = new EncodeRow[B] {
      override def apply[C <: SettableByName[C]](data: C, value: B): C = self(data, f(value))
    }
  }
}

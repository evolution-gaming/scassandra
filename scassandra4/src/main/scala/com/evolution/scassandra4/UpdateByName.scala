package com.evolution.scassandra4

import com.datastax.oss.driver.api.core.data.{GettableByName, SettableByName}

trait UpdateByName[-A] {

  def apply[D <: GettableByName & SettableByName[D]](
      data: D,
      name: String,
      value: A
  ): D

}

object UpdateByName {

  def apply[A: UpdateByName]: UpdateByName[A] = implicitly

  implicit def fromEncodeByName[A: EncodeByName]: UpdateByName[A] =
    new UpdateByName[A] {
      def apply[D <: GettableByName & SettableByName[D]](
          data: D,
          name: String,
          value: A
      ): D = EncodeByName[A].apply(data, name, value)
    }

  implicit final class Syntax[A](val self: UpdateByName[A]) extends AnyVal {

    def contramap[B](f: B => A): UpdateByName[B] = new UpdateByName[B] {
      def apply[D <: GettableByName & SettableByName[D]](
          data: D,
          name: String,
          value: B
      ): D = self(data, name, f(value))
    }

  }
}

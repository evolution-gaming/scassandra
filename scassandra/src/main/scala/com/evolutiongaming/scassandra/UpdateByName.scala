package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.cql.Bindable
import com.datastax.oss.driver.api.core.data.GettableByName

trait UpdateByName[-A] {
  def apply[D <: GettableByName & Bindable[D]](
      data: D,
      name: String,
      value: A
  ): D
}

object UpdateByName {
  def apply[A: UpdateByName]: UpdateByName[A] = implicitly

  implicit def fromEncodeByName[A: EncodeByName]: UpdateByName[A] =
    new UpdateByName[A] {
      def apply[D <: GettableByName & Bindable[D]](
          data: D,
          name: String,
          value: A
      ): D = EncodeByName[A].bindToData(data, name, value)
    }

  implicit final class Syntax[A](val self: UpdateByName[A]) extends AnyVal {
    def contramap[B](f: B => A): UpdateByName[B] = new UpdateByName[B] {
      def apply[D <: GettableByName & Bindable[D]](
          data: D,
          name: String,
          value: B
      ): D = self(data, name, f(value))
    }
  }
}

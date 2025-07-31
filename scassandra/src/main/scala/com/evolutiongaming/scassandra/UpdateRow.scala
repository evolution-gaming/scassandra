package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.cql.Bindable

trait UpdateRow[-A] {
  def apply[D <: Bindable[D]](
      data: D,
      value: A
  ): D
}

object UpdateRow {
  def apply[A: UpdateRow]: UpdateRow[A] = implicitly

  implicit def fromEncodeRow[A: EncodeRow]: UpdateRow[A] =
    new UpdateRow[A] {
      def apply[D <: Bindable[D]](
          data: D,
          value: A
      ): D = EncodeRow[A].apply(data, value)
    }

  implicit final class Syntax[A](private val self: UpdateRow[A]) extends AnyVal {
    def contramap[B](f: B => A): UpdateRow[B] = new UpdateRow[B] {
      def apply[D <: Bindable[D]](
          data: D,
          value: B
      ): D = self(data, f(value))
    }
  }
}

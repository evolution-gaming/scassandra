package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.cql.Bindable
import com.datastax.oss.driver.api.core.data.GettableByIndex

trait UpdateByIdx[-A] {
  def apply[D <: GettableByIndex & Bindable[D]](
      data: D,
      idx: Int,
      value: A
  ): D
}

object UpdateByIdx {
  def apply[A: UpdateByIdx]: UpdateByIdx[A] = implicitly

  implicit def fromEncodeByIdx[A: EncodeByIdx]: UpdateByIdx[A] =
    new UpdateByIdx[A] {
      def apply[D <: GettableByIndex & Bindable[D]](
          data: D,
          idx: Int,
          value: A
      ): D = EncodeByIdx[A].apply(data, idx, value)
    }

  implicit final class Syntax[A](val self: UpdateByIdx[A]) extends AnyVal {
    def contramap[B](f: B => A): UpdateByIdx[B] = new UpdateByIdx[B] {
      def apply[D <: GettableByIndex & Bindable[D]](
          data: D,
          idx: Int,
          value: B
      ): D = self(data, idx, f(value))
    }
  }
}

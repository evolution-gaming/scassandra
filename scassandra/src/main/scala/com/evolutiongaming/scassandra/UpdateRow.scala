package com.evolutiongaming.scassandra

import com.datastax.driver.core.{GettableData, SettableData}

trait UpdateRow[-A] {

  def apply[D <: GettableData & SettableData[D]](
      data: D,
      value: A
  ): D

}

object UpdateRow {

  def apply[A: UpdateRow]: UpdateRow[A] = implicitly

  implicit def fromEncodeRow[A: EncodeRow]: UpdateRow[A] =
    new UpdateRow[A] {
      def apply[D <: GettableData & SettableData[D]](
          data: D,
          value: A
      ): D = EncodeRow[A].apply(data, value)
    }

  implicit final class Syntax[A](val self: UpdateRow[A]) extends AnyVal {

    def contramap[B](f: B => A): UpdateRow[B] = new UpdateRow[B] {
      def apply[D <: GettableData & SettableData[D]](
          data: D,
          value: B
      ): D = self(data, f(value))
    }

  }
}

package com.evolutiongaming.scassandra

import com.datastax.driver.core.{GettableByIndexData, SettableData}

/** Codec for encoding and decoding values by index.
  *
  * Different from [[EncodeByIdx]] in that it can also take values while
  * encoding, thus allowing to _modify_ maps, lists and sets.
  *
  * @see
  *   [[EncodeByIdx]]
  * @see
  *   [[DecodeByIdx]]
  */
trait CodecByIdx[A] {

  def encode[D <: GettableByIndexData & SettableData[D]](
      data: D,
      idx: Int,
      value: A
  ): D

  def decode[D <: GettableByIndexData](
      data: D,
      idx: Int
  ): A

}

object CodecByIdx {

  def apply[A: CodecByIdx]: CodecByIdx[A] = implicitly

  implicit def fromEncodeDecode[A: EncodeByIdx: DecodeByIdx]: CodecByIdx[A] =
    new CodecByIdx[A] {

      def encode[D <: GettableByIndexData & SettableData[D]](
          data: D,
          idx: Int,
          value: A
      ): D = EncodeByIdx[A].apply(data, idx, value)

      def decode[D <: GettableByIndexData](
          data: D,
          idx: Int
      ): A = DecodeByIdx[A].apply(data, idx)
    }

  implicit class Ops[A](val self: CodecByIdx[A]) extends AnyVal {

    def map[B](ab: A => B)(ba: B => A): CodecByIdx[B] = new CodecByIdx[B] {

      def encode[D <: GettableByIndexData & SettableData[D]](
          data: D,
          idx: Int,
          value: B
      ): D = self.encode(data, idx, ba(value))

      def decode[D <: GettableByIndexData](
          data: D,
          idx: Int
      ): B = ab(self.decode(data, idx))
    }
  }

}

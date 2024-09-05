package com.evolutiongaming.scassandra

import com.datastax.driver.core.{GettableByNameData, SettableData}

/** Codec for encoding and decoding values by name.
  *
  * Different from [[EncodeByName]] in that it can also take values while
  * encoding, thus allowing to _modify_ maps, lists and sets.
  *
  * @see
  *   [[EncodeByName]]
  * @see
  *   [[DecodeByName]]
  */
trait CodecByName[A] {

  def encode[D <: GettableByNameData & SettableData[D]](
      data: D,
      name: String,
      value: A
  ): D

  def decode[D <: GettableByNameData](
      data: D,
      name: String
  ): A

}

object CodecByName {

  def apply[A: CodecByName]: CodecByName[A] = implicitly

  implicit def fromEncodeDecode[A: EncodeByName: DecodeByName]: CodecByName[A] =
    new CodecByName[A] {

      def encode[D <: GettableByNameData & SettableData[D]](
          data: D,
          name: String,
          value: A
      ): D = EncodeByName[A].apply(data, name, value)

      def decode[D <: GettableByNameData](
          data: D,
          name: String
      ): A = DecodeByName[A].apply(data, name)
    }

  implicit class Ops[A](val self: CodecByName[A]) extends AnyVal {

    def map[B](ab: A => B)(ba: B => A): CodecByName[B] =
      new CodecByName[B] {

        def encode[D <: GettableByNameData & SettableData[D]](
            data: D,
            name: String,
            value: B
        ): D = self.encode(data, name, ba(value))

        def decode[D <: GettableByNameData](
            data: D,
            name: String
        ): B = ab(self.decode(data, name))
      }
  }
}

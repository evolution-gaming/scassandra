package com.evolutiongaming.scassandra
import com.datastax.driver.core.{GettableData, SettableData}

/** Codec for encoding and decoding values to Cassandra row.
  *
  * Different from [[EncodeRow]] in that it can also take values while encoding,
  * thus allowing to _modify_ maps, lists and sets.
  *
  * @see
  *   [[EncodeRow]]
  * @see
  *   [[DecodeRow]]
  */
trait CodecRow[A] {

  def encode[D <: GettableData & SettableData[D]](data: D, value: A): D

  def decode[D <: GettableData](data: D): A

}

object CodecRow {

  def apply[A: CodecRow]: CodecRow[A] = implicitly

  implicit def fromEncodeDecode[A: EncodeRow: DecodeRow]: CodecRow[A] =
    new CodecRow[A] {

      def encode[D <: GettableData & SettableData[D]](data: D, value: A): D =
        EncodeRow[A].apply(data, value)

      def decode[D <: GettableData](data: D): A =
        DecodeRow[A].apply(data)
    }

  implicit class Ops[A](val self: CodecRow[A]) extends AnyVal {

    def map[B](ab: A => B)(ba: B => A): CodecRow[B] =
      new CodecRow[B] {

        def encode[D <: GettableData & SettableData[D]](data: D, value: B): D =
          self.encode(data, ba(value))

        def decode[D <: GettableData](data: D): B =
          ab(self.decode(data))
      }
  }
}

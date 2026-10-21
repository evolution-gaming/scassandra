package com.evolution.scassandra

import cats.Functor
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodec
import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import com.datastax.oss.driver.api.core.data.{CqlDuration, GettableByIndex}

import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters.*

/**
 * Reconstruct `A` data type from a column stored in [[GettableByIndex]] at the given
 * index.
 *
 * Note, that a null value is decoded as returned by the driver, i.e. `null` for objects,
 * zero or `false` for primitives and an empty set, while `BigDecimal` and `Array[Byte]`
 * fail. Decode nullable columns as `Option[A]`.
 */
trait DecodeByIdx[A] {

  def apply(data: GettableByIndex, idx: Int): A
}

object DecodeByIdx {

  def apply[A](
    implicit
    decode: DecodeByIdx[A],
  ): DecodeByIdx[A] = decode

  /**
   * Decodes with the given codec
   */
  def fromCodec[A](codec: TypeCodec[A]): DecodeByIdx[A] = (data: GettableByIndex, idx: Int) => {
    data.get(idx, codec)
  }

  /**
   * Decodes with a codec registered in the session for the given class
   */
  def fromClass[A](valueClass: Class[A]): DecodeByIdx[A] = (data: GettableByIndex, idx: Int) => {
    data.get(idx, valueClass)
  }

  /**
   * Decodes with a codec registered in the session for the given type
   */
  def fromGenericType[A](genericType: GenericType[A]): DecodeByIdx[A] = (data: GettableByIndex, idx: Int) => {
    data.get(idx, genericType)
  }

  implicit val functorDecodeByIdx: Functor[DecodeByIdx] = new Functor[DecodeByIdx] {
    override def map[A, B](fa: DecodeByIdx[A])(f: A => B): DecodeByIdx[B] = fa.map(f)
  }

  implicit def optDecodeByIdx[A](
    implicit
    decode: DecodeByIdx[A],
  ): DecodeByIdx[Option[A]] = (data: GettableByIndex, idx: Int) => {
    if (data.isNull(idx)) None
    else Some(decode(data, idx))
  }

  implicit val boolDecodeByIdx: DecodeByIdx[Boolean] = (data: GettableByIndex, idx: Int) => {
    data.getBoolean(idx)
  }

  implicit val strDecodeByIdx: DecodeByIdx[String] = (data: GettableByIndex, idx: Int) => {
    data.getString(idx)
  }

  implicit val shortDecodeByIdx: DecodeByIdx[Short] = (data: GettableByIndex, idx: Int) => {
    data.getShort(idx)
  }

  implicit val intDecodeByIdx: DecodeByIdx[Int] = (data: GettableByIndex, idx: Int) => {
    data.getInt(idx)
  }

  implicit val longDecodeByIdx: DecodeByIdx[Long] = (data: GettableByIndex, idx: Int) => {
    data.getLong(idx)
  }

  implicit val floatDecodeByIdx: DecodeByIdx[Float] = (data: GettableByIndex, idx: Int) => {
    data.getFloat(idx)
  }

  implicit val doubleDecodeByIdx: DecodeByIdx[Double] = (data: GettableByIndex, idx: Int) => {
    data.getDouble(idx)
  }

  implicit val instantDecodeByIdx: DecodeByIdx[Instant] = (data: GettableByIndex, idx: Int) => {
    data.getInstant(idx)
  }

  implicit val bigDecimalDecodeByIdx: DecodeByIdx[BigDecimal] = (data: GettableByIndex, idx: Int) => {
    BigDecimal(data.getBigDecimal(idx))
  }

  implicit val setStrDecodeByIdx: DecodeByIdx[Set[String]] = (data: GettableByIndex, idx: Int) => {
    data.getSet(idx, classOf[String]).asScala.toSet
  }

  implicit val bytesDecodeByIdx: DecodeByIdx[Array[Byte]] = (data: GettableByIndex, idx: Int) => {
    Bytes.toArray(data.getByteBuffer(idx))
  }

  implicit val durationDecodeByIdx: DecodeByIdx[CqlDuration] = (data: GettableByIndex, idx: Int) => {
    data.getCqlDuration(idx)
  }

  implicit val localDateDecodeByIdx: DecodeByIdx[LocalDate] = (data: GettableByIndex, idx: Int) => {
    data.getLocalDate(idx)
  }

  implicit class DecodeByIdxOps[A](val self: DecodeByIdx[A]) extends AnyVal {

    def map[B](f: A => B): DecodeByIdx[B] = (data: GettableByIndex, idx: Int) => f(self(data, idx))
  }
}

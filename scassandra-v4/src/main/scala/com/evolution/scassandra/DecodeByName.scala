package com.evolution.scassandra

import cats.Functor
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodec
import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import com.datastax.oss.driver.api.core.data.{CqlDuration, GettableByName}

import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters.*

/**
 * Reconstruct `A` data type from a named column stored in [[GettableByName]].
 *
 * Note, that a null value is decoded as returned by the driver, i.e. `null` for objects,
 * zero or `false` for primitives and an empty set, while `BigDecimal` and `Array[Byte]`
 * fail. Decode nullable columns as `Option[A]`.
 */
trait DecodeByName[A] {

  /**
   * Performs the decoding itself.
   *
   * Note, that the method might throw an exception if the required column is not found in
   * a row passed as `data` argument.
   *
   * @param data
   *   Data row returned by Cassandra.
   * @param name
   *   Name of a column in a row to get a value from.
   */
  def apply(data: GettableByName, name: String): A
}

object DecodeByName {

  def apply[A](
    implicit
    decode: DecodeByName[A],
  ): DecodeByName[A] = decode

  /**
   * Decodes with the given codec
   */
  def fromCodec[A](codec: TypeCodec[A]): DecodeByName[A] = (data: GettableByName, name: String) => {
    data.get(name, codec)
  }

  /**
   * Decodes with a codec registered in the session for the given class
   */
  def fromClass[A](valueClass: Class[A]): DecodeByName[A] = (data: GettableByName, name: String) => {
    data.get(name, valueClass)
  }

  /**
   * Decodes with a codec registered in the session for the given type
   */
  def fromGenericType[A](genericType: GenericType[A]): DecodeByName[A] = {
    (data: GettableByName, name: String) => data.get(name, genericType)
  }

  implicit val functorDecodeByName: Functor[DecodeByName] = new Functor[DecodeByName] {
    override def map[A, B](fa: DecodeByName[A])(f: A => B): DecodeByName[B] = fa.map(f)
  }

  implicit def optDecodeByName[A](
    implicit
    decode: DecodeByName[A],
  ): DecodeByName[Option[A]] = (data: GettableByName, name: String) => {
    if (data.isNull(name)) None
    else Some(decode(data, name))
  }

  implicit val boolDecodeByName: DecodeByName[Boolean] = (data: GettableByName, name: String) => {
    data.getBoolean(name)
  }

  implicit val strDecodeByName: DecodeByName[String] = (data: GettableByName, name: String) => {
    data.getString(name)
  }

  implicit val shortDecodeByName: DecodeByName[Short] = (data: GettableByName, name: String) => {
    data.getShort(name)
  }

  implicit val intDecodeByName: DecodeByName[Int] = (data: GettableByName, name: String) => {
    data.getInt(name)
  }

  implicit val longDecodeByName: DecodeByName[Long] = (data: GettableByName, name: String) => {
    data.getLong(name)
  }

  implicit val floatDecodeByName: DecodeByName[Float] = (data: GettableByName, name: String) => {
    data.getFloat(name)
  }

  implicit val doubleDecodeByName: DecodeByName[Double] = (data: GettableByName, name: String) => {
    data.getDouble(name)
  }

  implicit val instantDecodeByName: DecodeByName[Instant] = (data: GettableByName, name: String) => {
    data.getInstant(name)
  }

  implicit val bigDecimalDecodeByName: DecodeByName[BigDecimal] = (data: GettableByName, name: String) => {
    BigDecimal(data.getBigDecimal(name))
  }

  implicit val setStrDecodeByName: DecodeByName[Set[String]] = (data: GettableByName, name: String) => {
    data.getSet(name, classOf[String]).asScala.toSet
  }

  implicit val bytesDecodeByName: DecodeByName[Array[Byte]] = (data: GettableByName, name: String) => {
    Bytes.toArray(data.getByteBuffer(name))
  }

  implicit val durationDecodeByName: DecodeByName[CqlDuration] = (data: GettableByName, name: String) => {
    data.getCqlDuration(name)
  }

  implicit val localDateDecodeByName: DecodeByName[LocalDate] = (data: GettableByName, name: String) => {
    data.getLocalDate(name)
  }

  implicit class DecodeByNameOps[A](val self: DecodeByName[A]) extends AnyVal {
    def map[B](f: A => B): DecodeByName[B] = (data: GettableByName, name: String) => f(self(data, name))
  }
}

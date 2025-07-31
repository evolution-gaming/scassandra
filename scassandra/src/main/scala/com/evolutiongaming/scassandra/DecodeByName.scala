package com.evolutiongaming.scassandra

import java.time.{Instant, LocalDate as LocalDateJ}
import cats.Functor
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodecs
import com.datastax.oss.driver.api.core.data.{CqlDuration, GettableByName}
import com.evolutiongaming.util.ToScala

/** Reconstruct `A` data type from a named column stored in [[GettableByName]]. */
trait DecodeByName[A] {

  /** Performs the decoding itself.
    *
    * Note, that the method might throw an exception if the required column is
    * not found in a row passed as `data` argument.
    *
    * @param data Data row returned by Cassandra.
    * @param name Name of a column in a row to get a value from.
    */
  def apply(data: GettableByName, name: String): A
}

object DecodeByName {
  def apply[A](implicit decode: DecodeByName[A]): DecodeByName[A] = decode

  implicit val functorDecodeByName: Functor[DecodeByName] = new Functor[DecodeByName] {
    def map[A, B](fa: DecodeByName[A])(f: A => B): DecodeByName[B] = fa.map(f)
  }

  implicit def optDecodeByName[A](implicit decode: DecodeByName[A]): DecodeByName[Option[A]] =
    (data: GettableByName, name: String) => {
      if (data.isNull(name)) None
      else Some(decode(data, name))
    }

  implicit val boolDecodeByName: DecodeByName[Boolean] =
    (data: GettableByName, name: String) => data.getBoolean(name)

  implicit val strDecodeByName: DecodeByName[String] =
    (data: GettableByName, name: String) => data.getString(name)

  implicit val shortDecodeByName: DecodeByName[Short] =
    (data: GettableByName, name: String) => data.getShort(name)

  implicit val intDecodeByName: DecodeByName[Int] =
    (data: GettableByName, name: String) => data.getInt(name)

  implicit val longDecodeByName: DecodeByName[Long] =
    (data: GettableByName, name: String) => data.getLong(name)

  implicit val floatDecodeByName: DecodeByName[Float] =
    (data: GettableByName, name: String) => data.getFloat(name)

  implicit val doubleDecodeByName: DecodeByName[Double] =
    (data: GettableByName, name: String) => data.getDouble(name)

  implicit val instantDecodeByName: DecodeByName[Instant] =
    (data: GettableByName, name: String) => data.getInstant(name)

  implicit val bigDecimalDecodeByName: DecodeByName[BigDecimal] =
    (data: GettableByName, name: String) => BigDecimal(data.getBigDecimal(name))

  implicit val setStrDecodeByName: DecodeByName[Set[String]] =
    (data: GettableByName, name: String) => {
      val set = data.getSet(name, classOf[String])
      ToScala.from(set).toSet
    }

  implicit val bytesDecodeByName: DecodeByName[Array[Byte]] =
    (data: GettableByName, name: String) => {
      val bytes = data.getBytesUnsafe(name)
      bytes.array()
    }

  implicit val durationDecodeByName: DecodeByName[CqlDuration] =
    (data: GettableByName, name: String) => data.get(name, TypeCodecs.DURATION)

  implicit val localDateDecodeByName: DecodeByName[LocalDateJ] =
    (data: GettableByName, name: String) => data.getLocalDate(name)

  object Ops {
    implicit class GettableByNameOps(private val self: GettableByName) extends AnyVal {
      def decode[A](name: String)(implicit decode: DecodeByName[A]): A = decode(self, name)
    }
  }

  implicit class DecodeByNameOps[A](private val self: DecodeByName[A]) extends AnyVal {
    def map[B](f: A => B): DecodeByName[B] = (data: GettableByName, name: String) => f(self(data, name))
  }
}

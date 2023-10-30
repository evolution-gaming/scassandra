package com.evolutiongaming.scassandra

import java.time.{Instant, LocalDate => LocalDateJ}

import cats.Functor
import com.datastax.driver.core.{Duration, GettableByNameData, LocalDate, TypeCodec}
import com.evolutiongaming.util.ToScala

/** Reconstruct `A` data type from a named column stored in [[GettableByNameData]]. */
trait DecodeByName[A] {

  /** Performs the decoding itself.
    *
    * Note, that the method might throw an exception if the required column is
    * not found in a row passed as `data` argument.
    *
    * @param data Data row returned by Cassandra.
    * @param name Name of a column in a row to get a value from.
    */
  def apply(data: GettableByNameData, name: String): A
}

object DecodeByName {

  def apply[A](implicit decode: DecodeByName[A]): DecodeByName[A] = decode


  implicit val functorDecodeByName: Functor[DecodeByName] = new Functor[DecodeByName] {
    def map[A, B](fa: DecodeByName[A])(f: A => B) = fa.map(f)
  }


  implicit def optDecodeByName[A](implicit decode: DecodeByName[A]): DecodeByName[Option[A]] = {
    (data: GettableByNameData, name: String) => {
      if (data.isNull(name)) None
      else Some(decode(data, name))
    }
  }


  implicit val boolDecodeByName: DecodeByName[Boolean] = {
    (data: GettableByNameData, name: String) => data.getBool(name)
  }


  implicit val strDecodeByName: DecodeByName[String] = {
    (data: GettableByNameData, name: String) => data.getString(name)
  }


  implicit val shortDecodeByName: DecodeByName[Short] = {
    (data: GettableByNameData, name: String) => data.getShort(name)
  }


  implicit val intDecodeByName: DecodeByName[Int] = {
    (data: GettableByNameData, name: String) => data.getInt(name)
  }


  implicit val longDecodeByName: DecodeByName[Long] = {
    (data: GettableByNameData, name: String) => data.getLong(name)
  }


  implicit val floatDecodeByName: DecodeByName[Float] = {
    (data: GettableByNameData, name: String) => data.getFloat(name)
  }


  implicit val doubleDecodeByName: DecodeByName[Double] = {
    (data: GettableByNameData, name: String) => data.getDouble(name)
  }


  implicit val instantDecodeByName: DecodeByName[Instant] = {
    (data: GettableByNameData, name: String) => {
      val timestamp = data.getTimestamp(name)
      timestamp.toInstant
    }
  }


  implicit val bigDecimalDecodeByName: DecodeByName[BigDecimal] = {
    (data: GettableByNameData, name: String) => BigDecimal(data.getDecimal(name))
  }


  implicit val setStrDecodeByName: DecodeByName[Set[String]] = {
    (data: GettableByNameData, name: String) => {
      val set = data.getSet(name, classOf[String])
      ToScala.from(set).toSet
    }
  }

  implicit val bytesDecodeByName: DecodeByName[Array[Byte]] = {
    (data: GettableByNameData, name: String) => {
      val bytes = data.getBytes(name)
      bytes.array()
    }
  }

  implicit val durationDecodeByName: DecodeByName[Duration] = {
    (data: GettableByNameData, name: String) => {
      data.get(name, TypeCodec.duration())
    }
  }

  implicit val localDateDecodeByName: DecodeByName[LocalDate] = {
    (data: GettableByNameData, name: String) => data.getDate(name)
  }

  implicit val localDateJDecodeByName: DecodeByName[LocalDateJ] = {
    DecodeByName[LocalDate].map { a => LocalDateJ.ofEpochDay(a.getDaysSinceEpoch.toLong)}
  }


  object Ops {

    implicit class GettableByNameDataOps(val self: GettableByNameData) extends AnyVal {

      def decode[A](name: String)(implicit decode: DecodeByName[A]): A = decode(self, name)
    }
  }


  implicit class DecodeByNameOps[A](val self: DecodeByName[A]) extends AnyVal {
    def map[B](f: A => B): DecodeByName[B] = (data: GettableByNameData, name: String) => f(self(data, name))
  }
}

package com.evolutiongaming.scassandra

import java.time.Instant

import com.datastax.driver.core.GettableByNameData
import com.evolutiongaming.util.ToScala

trait DecodeByName[A] extends { self =>

  def apply(data: GettableByNameData, name: String): A


  final def map[B](f: A => B): DecodeByName[B] = new DecodeByName[B] {
    def apply(data: GettableByNameData, name: String) = f(self(data, name))
  }
}

object DecodeByName {

  def apply[A](implicit decode: DecodeByName[A]): DecodeByName[A] = decode

  implicit def opt[A](implicit decode: DecodeByName[A]): DecodeByName[Option[A]] = new DecodeByName[Option[A]] {

    def apply(data: GettableByNameData, name: String) = {
      if (data.isNull(name)) None
      else Some(decode(data, name))
    }
  }


  implicit val BoolImpl: DecodeByName[Boolean] = new DecodeByName[Boolean] {
    def apply(data: GettableByNameData, name: String) = data.getBool(name)
  }

  implicit val BoolOptImpl: DecodeByName[Option[Boolean]] = opt[Boolean]


  implicit val StrImpl: DecodeByName[String] = new DecodeByName[String] {
    def apply(data: GettableByNameData, name: String) = data.getString(name)
  }

  implicit val StrOptImpl: DecodeByName[Option[String]] = opt[String]


  implicit val ShortImpl: DecodeByName[Short] = new DecodeByName[Short] {
    def apply(data: GettableByNameData, name: String) = data.getShort(name)
  }

  implicit val ShortOptImpl: DecodeByName[Option[Short]] = opt[Short]


  implicit val IntImpl: DecodeByName[Int] = new DecodeByName[Int] {
    def apply(data: GettableByNameData, name: String) = data.getInt(name)
  }

  implicit val IntOptImpl: DecodeByName[Option[Int]] = opt[Int]


  implicit val LongImpl: DecodeByName[Long] = new DecodeByName[Long] {
    def apply(data: GettableByNameData, name: String) = data.getLong(name)
  }

  implicit val LongOptImpl: DecodeByName[Option[Long]] = opt[Long]


  implicit val FloatImpl: DecodeByName[Float] = new DecodeByName[Float] {
    def apply(data: GettableByNameData, name: String) = data.getFloat(name)
  }

  implicit val FloatOptImpl: DecodeByName[Option[Float]] = opt[Float]


  implicit val DoubleImpl: DecodeByName[Double] = new DecodeByName[Double] {
    def apply(data: GettableByNameData, name: String) = data.getDouble(name)
  }

  implicit val DoubleOptImpl: DecodeByName[Option[Double]] = opt[Double]


  implicit val InstantImpl: DecodeByName[Instant] = new DecodeByName[Instant] {
    def apply(data: GettableByNameData, name: String) = {
      val timestamp = data.getTimestamp(name)
      timestamp.toInstant
    }
  }

  implicit val InstantOptImpl: DecodeByName[Option[Instant]] = opt[Instant]


  implicit val BigDecimalImpl: DecodeByName[BigDecimal] = new DecodeByName[BigDecimal] {
    def apply(data: GettableByNameData, name: String) = BigDecimal(data.getDecimal(name))
  }

  implicit val BigDecimalOptImpl: DecodeByName[Option[BigDecimal]] = opt[BigDecimal]


  implicit val SetStrImpl: DecodeByName[Set[String]] = new DecodeByName[Set[String]] {
    def apply(data: GettableByNameData, name: String) = {
      val set = data.getSet(name, classOf[String])
      ToScala.from(set).toSet
    }
  }

  implicit val BytesImpl: DecodeByName[Array[Byte]] = new DecodeByName[Array[Byte]] {
    def apply(data: GettableByNameData, name: String) = {
      val bytes = data.getBytes(name)
      bytes.array()
    }
  }


  object Ops {

    implicit class GettableByNameDataOps(val self: GettableByNameData) extends AnyVal {

      def decode[A](name: String)(implicit decode: DecodeByName[A]): A = decode(self, name)
    }
  }
}
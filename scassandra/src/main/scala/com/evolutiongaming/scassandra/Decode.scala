package com.evolutiongaming.scassandra

import java.time.Instant

import com.datastax.driver.core.Row

import scala.collection.JavaConverters._

// TODO check performance of binding `by name`
// TODO cover with tests
// TODO add codecs for all supported types
trait Decode[A] extends { self =>

  def apply(row: Row, name: String): A


  final def map[B](f: A => B): Decode[B] = new Decode[B] {
    def apply(row: Row, name: String) = f(self(row, name))
  }
}

object Decode {

  def apply[A](implicit decode: Decode[A]): Decode[A] = decode

  implicit def opt[A](implicit decode: Decode[A]): Decode[Option[A]] = new Decode[Option[A]] {

    def apply(row: Row, name: String) = {
      if (row.isNull(name)) None else Some(decode(row, name))
    }
  }

  
  implicit val BoolImpl: Decode[Boolean] = new Decode[Boolean] {
    def apply(row: Row, name: String) = row.getBool(name)
  }

  implicit val BoolOptImpl: Decode[Option[Boolean]] = opt[Boolean]


  implicit val StrImpl: Decode[String] = new Decode[String] {
    def apply(row: Row, name: String) = row.getString(name)
  }

  implicit val StrOptImpl: Decode[Option[String]] = opt[String]


  implicit val ShortImpl: Decode[Short] = new Decode[Short] {
    def apply(row: Row, name: String) = row.getShort(name)
  }

  implicit val ShortOptImpl: Decode[Option[Short]] = opt[Short]


  implicit val IntImpl: Decode[Int] = new Decode[Int] {
    def apply(row: Row, name: String) = row.getInt(name)
  }

  implicit val IntOptImpl: Decode[Option[Int]] = opt[Int]


  implicit val LongImpl: Decode[Long] = new Decode[Long] {
    def apply(row: Row, name: String) = row.getLong(name)
  }

  implicit val LongOptImpl: Decode[Option[Long]] = opt[Long]


  implicit val FloatImpl: Decode[Float] = new Decode[Float] {
    def apply(row: Row, name: String) = row.getFloat(name)
  }

  implicit val FloatOptImpl: Decode[Option[Float]] = opt[Float]


  implicit val DoubleImpl: Decode[Double] = new Decode[Double] {
    def apply(row: Row, name: String) = row.getDouble(name)
  }

  implicit val DoubleOptImpl: Decode[Option[Double]] = opt[Double]


  implicit val InstantImpl: Decode[Instant] = new Decode[Instant] {
    def apply(row: Row, name: String) = {
      val timestamp = row.getTimestamp(name)
      timestamp.toInstant
    }
  }

  implicit val InstantOptImpl: Decode[Option[Instant]] = opt[Instant]


  implicit val BigDecimalImpl: Decode[BigDecimal] = new Decode[BigDecimal] {
    def apply(row: Row, name: String) = BigDecimal(row.getDecimal(name))
  }

  implicit val BigDecimalOptImpl: Decode[Option[BigDecimal]] = opt[BigDecimal]


  implicit val SetStrImpl: Decode[Set[String]] = new Decode[Set[String]] {
    def apply(row: Row, name: String) = {
      val set = row.getSet(name, classOf[String])
      set.asScala.toSet
    }
  }

  implicit val BytesImpl: Decode[Array[Byte]] = new Decode[Array[Byte]] {
    def apply(row: Row, name: String) = {
      val bytes = row.getBytes(name)
      bytes.array()
    }
  }


  object Ops {

    implicit class RowOps(val self: Row) extends AnyVal {

      def decode[T](name: String)(implicit decode: Decode[T]): T = decode(self, name)
    }
  }
}
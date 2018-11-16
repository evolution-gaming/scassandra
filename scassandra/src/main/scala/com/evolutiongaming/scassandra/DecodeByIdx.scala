package com.evolutiongaming.scassandra

import java.time.Instant

import com.datastax.driver.core.GettableByIndexData

import scala.collection.JavaConverters._

trait DecodeByIdx[A] extends { self =>

  def apply(data: GettableByIndexData, idx: Int): A


  final def map[B](f: A => B): DecodeByIdx[B] = new DecodeByIdx[B] {
    def apply(data: GettableByIndexData, idx: Int) = f(self(data, idx))
  }
}

object DecodeByIdx {

  def apply[A](implicit decode: DecodeByIdx[A]): DecodeByIdx[A] = decode

  implicit def opt[A](implicit decode: DecodeByIdx[A]): DecodeByIdx[Option[A]] = new DecodeByIdx[Option[A]] {

    def apply(data: GettableByIndexData, idx: Int) = {
      if (data.isNull(idx)) None
      else Some(decode(data, idx))
    }
  }


  implicit val BoolImpl: DecodeByIdx[Boolean] = new DecodeByIdx[Boolean] {
    def apply(data: GettableByIndexData, idx: Int) = data.getBool(idx)
  }

  implicit val BoolOptImpl: DecodeByIdx[Option[Boolean]] = opt[Boolean]


  implicit val StrImpl: DecodeByIdx[String] = new DecodeByIdx[String] {
    def apply(data: GettableByIndexData, idx: Int) = data.getString(idx)
  }

  implicit val StrOptImpl: DecodeByIdx[Option[String]] = opt[String]


  implicit val ShortImpl: DecodeByIdx[Short] = new DecodeByIdx[Short] {
    def apply(data: GettableByIndexData, idx: Int) = data.getShort(idx)
  }

  implicit val ShortOptImpl: DecodeByIdx[Option[Short]] = opt[Short]


  implicit val IntImpl: DecodeByIdx[Int] = new DecodeByIdx[Int] {
    def apply(data: GettableByIndexData, idx: Int) = data.getInt(idx)
  }

  implicit val IntOptImpl: DecodeByIdx[Option[Int]] = opt[Int]


  implicit val LongImpl: DecodeByIdx[Long] = new DecodeByIdx[Long] {
    def apply(data: GettableByIndexData, idx: Int) = data.getLong(idx)
  }

  implicit val LongOptImpl: DecodeByIdx[Option[Long]] = opt[Long]


  implicit val FloatImpl: DecodeByIdx[Float] = new DecodeByIdx[Float] {
    def apply(data: GettableByIndexData, idx: Int) = data.getFloat(idx)
  }

  implicit val FloatOptImpl: DecodeByIdx[Option[Float]] = opt[Float]


  implicit val DoubleImpl: DecodeByIdx[Double] = new DecodeByIdx[Double] {
    def apply(data: GettableByIndexData, idx: Int) = data.getDouble(idx)
  }

  implicit val DoubleOptImpl: DecodeByIdx[Option[Double]] = opt[Double]


  implicit val InstantImpl: DecodeByIdx[Instant] = new DecodeByIdx[Instant] {
    def apply(data: GettableByIndexData, idx: Int) = {
      val timestamp = data.getTimestamp(idx)
      timestamp.toInstant
    }
  }

  implicit val InstantOptImpl: DecodeByIdx[Option[Instant]] = opt[Instant]


  implicit val BigDecimalImpl: DecodeByIdx[BigDecimal] = new DecodeByIdx[BigDecimal] {
    def apply(data: GettableByIndexData, idx: Int) = BigDecimal(data.getDecimal(idx))
  }

  implicit val BigDecimalOptImpl: DecodeByIdx[Option[BigDecimal]] = opt[BigDecimal]


  implicit val SetStrImpl: DecodeByIdx[Set[String]] = new DecodeByIdx[Set[String]] {
    def apply(data: GettableByIndexData, idx: Int) = {
      val set = data.getSet(idx, classOf[String])
      set.asScala.toSet
    }
  }

  implicit val BytesImpl: DecodeByIdx[Array[Byte]] = new DecodeByIdx[Array[Byte]] {
    def apply(data: GettableByIndexData, idx: Int) = {
      val bytes = data.getBytes(idx)
      bytes.array()
    }
  }


  object Ops {

    implicit class GettableByIndexDataOps(val self: GettableByIndexData) extends AnyVal {

      def decodeAt[A](idx: Int)(implicit decode: DecodeByIdx[A]): A = decode(self, idx)
    }
  }
}
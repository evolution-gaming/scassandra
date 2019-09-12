package com.evolutiongaming.scassandra

import java.nio.ByteBuffer
import java.time.Instant
import java.util.Date

import com.datastax.driver.core.SettableData
import com.evolutiongaming.util.ToJava

trait EncodeByIdx[-A] { self =>

  def apply[B <: SettableData[B]](data: B, idx: Int, value: A): B


  final def imap[B](f: B => A): EncodeByIdx[B] = new EncodeByIdx[B] {
    def apply[C <: SettableData[C]](data: C, idx: Int, value: B) = self(data, idx, f(value))
  }
}

object EncodeByIdx {

  def apply[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[A] = encode

  implicit def opt[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[Option[A]] = new EncodeByIdx[Option[A]] {

    def apply[B <: SettableData[B]](data: B, idx: Int, value: Option[A]) = {
      value match {
        case Some(value) => encode(data, idx, value)
        case None        => data.setToNull(idx)
      }
    }
  }


  implicit val BoolImpl: EncodeByIdx[Boolean] = new EncodeByIdx[Boolean] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Boolean) = data.setBool(idx, value)
  }

  implicit val BoolOptImpl: EncodeByIdx[Option[Boolean]] = opt[Boolean]


  implicit val StrImpl: EncodeByIdx[String] = new EncodeByIdx[String] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: String) = data.setString(idx, value)
  }

  implicit val StrOptImpl: EncodeByIdx[Option[String]] = opt[String]


  implicit val ShortImpl: EncodeByIdx[Short] = new EncodeByIdx[Short] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Short) = data.setShort(idx, value)
  }

  implicit val ShortOptImpl: EncodeByIdx[Option[Short]] = opt[Short]


  implicit val IntImpl: EncodeByIdx[Int] = new EncodeByIdx[Int] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Int) = data.setInt(idx, value)
  }

  implicit val IntOptImpl: EncodeByIdx[Option[Int]] = opt[Int]


  implicit val LongImpl: EncodeByIdx[Long] = new EncodeByIdx[Long] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Long) = data.setLong(idx, value)
  }

  implicit val LongOptImpl: EncodeByIdx[Option[Long]] = opt[Long]


  implicit val FloatImpl: EncodeByIdx[Float] = new EncodeByIdx[Float] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Float) = data.setFloat(idx, value)
  }

  implicit val FloatOptImpl: EncodeByIdx[Option[Float]] = opt[Float]


  implicit val DoubleImpl: EncodeByIdx[Double] = new EncodeByIdx[Double] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Double) = data.setDouble(idx, value)
  }

  implicit val DoubleOptImpl: EncodeByIdx[Option[Double]] = opt[Double]


  implicit val InstantImpl: EncodeByIdx[Instant] = new EncodeByIdx[Instant] {

    def apply[B <: SettableData[B]](data: B, idx: Int, value: Instant) = {
      val timestamp = Date.from(value)
      data.setTimestamp(idx, timestamp)
    }
  }

  implicit val InstantOptImpl: EncodeByIdx[Option[Instant]] = opt[Instant]


  implicit val BigDecimalImpl: EncodeByIdx[BigDecimal] = new EncodeByIdx[BigDecimal] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: BigDecimal) = {
      data.setDecimal(idx, value.bigDecimal)
    }
  }

  implicit val BigDecimalOptImpl: EncodeByIdx[Option[BigDecimal]] = opt[BigDecimal]


  implicit val SetStrImpl: EncodeByIdx[Set[String]] = new EncodeByIdx[Set[String]] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Set[String]) = {
      val set = ToJava.from(value)
      data.setSet(idx, set, classOf[String])
    }
  }

  implicit val BytesImpl: EncodeByIdx[Array[Byte]] = new EncodeByIdx[Array[Byte]] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Array[Byte]) = {
      val bytes = ByteBuffer.wrap(value)
      data.setBytes(idx, bytes)
    }
  }

  object Ops {

    implicit class SettableDataOps[A <: SettableData[A]](val self: A) extends AnyVal {

      def encodeAt[B](idx: Int, value: B)(implicit encode: EncodeByIdx[B]): A = {
        encode(self, idx, value)
      }
    }
  }
}


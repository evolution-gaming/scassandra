package com.evolutiongaming.scassandra

import java.nio.ByteBuffer
import java.time.Instant
import java.util.Date

import com.datastax.driver.core.SettableData
import com.evolutiongaming.util.ToJava

trait EncodeByName[-A] { self =>

  def apply[B <: SettableData[B]](data: B, name: String, value: A): B


  final def imap[B](f: B => A): EncodeByName[B] = new EncodeByName[B] {
    def apply[C <: SettableData[C]](data: C, name: String, value: B) = self(data, name, f(value))
  }
}

object EncodeByName {

  def apply[A](implicit encode: EncodeByName[A]): EncodeByName[A] = encode

  implicit def opt[A](implicit encode: EncodeByName[A]): EncodeByName[Option[A]] = noneAsNull[A]
  

  def noneAsNull[A](implicit encode: EncodeByName[A]): EncodeByName[Option[A]] = new EncodeByName[Option[A]] {

    def apply[B <: SettableData[B]](data: B, name: String, value: Option[A]) = {
      value match {
        case Some(value) => encode(data, name, value)
        case None        => data.setToNull(name)
      }
    }
  }

  def noneAsUnset[A](implicit encode: EncodeByName[A]): EncodeByName[Option[A]] = new EncodeByName[Option[A]] {

    def apply[B <: SettableData[B]](data: B, name: String, value: Option[A]) = {
      value match {
        case Some(value) => encode(data, name, value)
        case None        => data
      }
    }
  }


  implicit val BoolImpl: EncodeByName[Boolean] = new EncodeByName[Boolean] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Boolean) = data.setBool(name, value)
  }

  implicit val BoolOptImpl: EncodeByName[Option[Boolean]] = opt[Boolean]


  implicit val StrImpl: EncodeByName[String] = new EncodeByName[String] {
    def apply[B <: SettableData[B]](data: B, name: String, value: String) = data.setString(name, value)
  }

  implicit val StrOptImpl: EncodeByName[Option[String]] = opt[String]


  implicit val ShortImpl: EncodeByName[Short] = new EncodeByName[Short] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Short) = data.setShort(name, value)
  }

  implicit val ShortOptImpl: EncodeByName[Option[Short]] = opt[Short]


  implicit val IntImpl: EncodeByName[Int] = new EncodeByName[Int] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Int) = data.setInt(name, value)
  }

  implicit val IntOptImpl: EncodeByName[Option[Int]] = opt[Int]


  implicit val LongImpl: EncodeByName[Long] = new EncodeByName[Long] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Long) = data.setLong(name, value)
  }

  implicit val LongOptImpl: EncodeByName[Option[Long]] = opt[Long]


  implicit val FloatImpl: EncodeByName[Float] = new EncodeByName[Float] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Float) = data.setFloat(name, value)
  }

  implicit val FloatOptImpl: EncodeByName[Option[Float]] = opt[Float]


  implicit val DoubleImpl: EncodeByName[Double] = new EncodeByName[Double] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Double) = data.setDouble(name, value)
  }

  implicit val DoubleOptImpl: EncodeByName[Option[Double]] = opt[Double]


  implicit val InstantImpl: EncodeByName[Instant] = new EncodeByName[Instant] {

    def apply[B <: SettableData[B]](data: B, name: String, value: Instant) = {
      val timestamp = Date.from(value)
      data.setTimestamp(name, timestamp)
    }
  }

  implicit val InstantOptImpl: EncodeByName[Option[Instant]] = opt[Instant]


  implicit val BigDecimalImpl: EncodeByName[BigDecimal] = new EncodeByName[BigDecimal] {
    def apply[B <: SettableData[B]](data: B, name: String, value: BigDecimal) = {
      data.setDecimal(name, value.bigDecimal)
    }
  }

  implicit val BigDecimalOptImpl: EncodeByName[Option[BigDecimal]] = opt[BigDecimal]


  implicit val SetStrImpl: EncodeByName[Set[String]] = new EncodeByName[Set[String]] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Set[String]) = {
      val set = ToJava.from(value)
      data.setSet(name, set, classOf[String])
    }
  }

  implicit val BytesImpl: EncodeByName[Array[Byte]] = new EncodeByName[Array[Byte]] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Array[Byte]) = {
      val bytes = ByteBuffer.wrap(value)
      data.setBytes(name, bytes)
    }
  }

  object Ops {

    implicit class SettableDataOps[A <: SettableData[A]](val self: A) extends AnyVal {

      def encode[B](name: String, value: B)(implicit encode: EncodeByName[B]): A = {
        encode(self, name, value)
      }
    }
  }
}


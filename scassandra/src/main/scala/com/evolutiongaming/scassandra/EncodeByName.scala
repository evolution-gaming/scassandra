package com.evolutiongaming.scassandra

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate => LocalDateJ}
import java.util.Date

import cats.Contravariant
import com.datastax.driver.core.{Duration, LocalDate, SettableData, TypeCodec}
import com.evolutiongaming.util.ToJava

trait EncodeByName[-A] {

  def apply[B <: SettableData[B]](data: B, name: String, value: A): B
}

object EncodeByName {

  implicit val contravariantEncodeByName: Contravariant[EncodeByName] = new Contravariant[EncodeByName] {
    def contramap[A, B](fa: EncodeByName[A])(f: B => A) = fa.contramap(f)
  }


  def apply[A](implicit encode: EncodeByName[A]): EncodeByName[A] = encode

  implicit def optEncodeByName[A](implicit encode: EncodeByName[A]): EncodeByName[Option[A]] = noneAsNull[A]


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


  implicit val boolEncodeByName: EncodeByName[Boolean] = new EncodeByName[Boolean] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Boolean) = data.setBool(name, value)
  }


  implicit val StrEncodeByName: EncodeByName[String] = new EncodeByName[String] {
    def apply[B <: SettableData[B]](data: B, name: String, value: String) = data.setString(name, value)
  }


  implicit val shortEncodeByName: EncodeByName[Short] = new EncodeByName[Short] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Short) = data.setShort(name, value)
  }


  implicit val intEncodeByName: EncodeByName[Int] = new EncodeByName[Int] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Int) = data.setInt(name, value)
  }


  implicit val longEncodeByName: EncodeByName[Long] = new EncodeByName[Long] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Long) = data.setLong(name, value)
  }


  implicit val floatEncodeByName: EncodeByName[Float] = new EncodeByName[Float] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Float) = data.setFloat(name, value)
  }


  implicit val doubleEncodeByName: EncodeByName[Double] = new EncodeByName[Double] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Double) = data.setDouble(name, value)
  }


  implicit val instantEncodeByName: EncodeByName[Instant] = new EncodeByName[Instant] {

    def apply[B <: SettableData[B]](data: B, name: String, value: Instant) = {
      val timestamp = Date.from(value)
      data.setTimestamp(name, timestamp)
    }
  }


  implicit val bigDecimalEncodeByName: EncodeByName[BigDecimal] = new EncodeByName[BigDecimal] {
    def apply[B <: SettableData[B]](data: B, name: String, value: BigDecimal) = {
      data.setDecimal(name, value.bigDecimal)
    }
  }


  implicit val setStrEncodeByName: EncodeByName[Set[String]] = new EncodeByName[Set[String]] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Set[String]) = {
      val set = ToJava.from(value)
      data.setSet(name, set, classOf[String])
    }
  }

  implicit val bytesEncodeByName: EncodeByName[Array[Byte]] = new EncodeByName[Array[Byte]] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Array[Byte]) = {
      val bytes = ByteBuffer.wrap(value)
      data.setBytes(name, bytes)
    }
  }

  implicit val durationEncodeByName: EncodeByName[Duration] = new EncodeByName[Duration] {
    def apply[B <: SettableData[B]](data: B, name: String, value: Duration) = {
      data.set(name, value, TypeCodec.duration())
    }
  }

  implicit val localDateEncodeByName: EncodeByName[LocalDate] = new EncodeByName[LocalDate] {

    def apply[B <: SettableData[B]](data: B, name: String, value: LocalDate) = {
      data.setDate(name, value)
    }
  }

  implicit val localDateJEncodeByName: EncodeByName[LocalDateJ] = {
    EncodeByName[LocalDate].contramap { (a: LocalDateJ) =>
      LocalDate.fromDaysSinceEpoch(a.toEpochDay.toInt)
    }
  }


  object Ops {

    implicit class SettableDataOps[A <: SettableData[A]](val self: A) extends AnyVal {

      def encode[B](name: String, value: B)(implicit encode: EncodeByName[B]): A = {
        encode(self, name, value)
      }
    }
  }


  implicit class EncodeByNameOps[A](val self: EncodeByName[A]) extends AnyVal {

    def contramap[B](f: B => A): EncodeByName[B] = new EncodeByName[B] {
      def apply[C <: SettableData[C]](data: C, name: String, value: B) = self(data, name, f(value))
    }
  }
}


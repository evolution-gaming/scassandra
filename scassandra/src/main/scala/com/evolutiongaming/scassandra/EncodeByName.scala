package com.evolutiongaming.scassandra

import cats.Contravariant
import com.datastax.driver.core.{Duration, LocalDate, SettableData, TypeCodec}

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate as LocalDateJ}
import java.util.Date
import scala.jdk.CollectionConverters.*

trait EncodeByName[-A] {

  def apply[B <: SettableData[B]](data: B, name: String, value: A): B
}

object EncodeByName {

  implicit val contravariantEncodeByName: Contravariant[EncodeByName] = new Contravariant[EncodeByName] {
    override def contramap[A, B](fa: EncodeByName[A])(f: B => A): EncodeByName[B] = fa.contramap(f)
  }

  def apply[A](
    implicit
    encode: EncodeByName[A],
  ): EncodeByName[A] = encode

  implicit def optEncodeByName[A](
    implicit
    encode: EncodeByName[A],
  ): EncodeByName[Option[A]] = noneAsNull[A]

  def noneAsNull[A](
    implicit
    encode: EncodeByName[A],
  ): EncodeByName[Option[A]] = new EncodeByName[Option[A]] {

    override def apply[B <: SettableData[B]](data: B, name: String, value: Option[A]): B = {
      value match {
        case Some(value) => encode(data, name, value)
        case None => data.setToNull(name)
      }
    }
  }

  def noneAsUnset[A](
    implicit
    encode: EncodeByName[A],
  ): EncodeByName[Option[A]] = new EncodeByName[Option[A]] {

    override def apply[B <: SettableData[B]](data: B, name: String, value: Option[A]): B = {
      value match {
        case Some(value) => encode(data, name, value)
        case None => data
      }
    }
  }

  implicit val boolEncodeByName: EncodeByName[Boolean] = new EncodeByName[Boolean] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Boolean): B = {
      data.setBool(name, value)
    }
  }

  implicit val StrEncodeByName: EncodeByName[String] = new EncodeByName[String] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: String): B = {
      data.setString(name, value)
    }
  }

  implicit val shortEncodeByName: EncodeByName[Short] = new EncodeByName[Short] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Short): B = {
      data.setShort(name, value)
    }
  }

  implicit val intEncodeByName: EncodeByName[Int] = new EncodeByName[Int] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Int): B = {
      data.setInt(name, value)
    }
  }

  implicit val longEncodeByName: EncodeByName[Long] = new EncodeByName[Long] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Long): B = {
      data.setLong(name, value)
    }
  }

  implicit val floatEncodeByName: EncodeByName[Float] = new EncodeByName[Float] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Float): B = {
      data.setFloat(name, value)
    }
  }

  implicit val doubleEncodeByName: EncodeByName[Double] = new EncodeByName[Double] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Double): B = {
      data.setDouble(name, value)
    }
  }

  implicit val instantEncodeByName: EncodeByName[Instant] = new EncodeByName[Instant] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Instant): B = {
      data.setTimestamp(name, Date.from(value))
    }
  }

  implicit val bigDecimalEncodeByName: EncodeByName[BigDecimal] = new EncodeByName[BigDecimal] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: BigDecimal): B = {
      data.setDecimal(name, value.bigDecimal)
    }
  }

  implicit val setStrEncodeByName: EncodeByName[Set[String]] = new EncodeByName[Set[String]] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Set[String]): B = {
      data.setSet(name, value.asJava, classOf[String])
    }
  }

  implicit val bytesEncodeByName: EncodeByName[Array[Byte]] = new EncodeByName[Array[Byte]] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Array[Byte]): B = {
      data.setBytes(name, ByteBuffer.wrap(value))
    }
  }

  implicit val durationEncodeByName: EncodeByName[Duration] = new EncodeByName[Duration] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: Duration): B = {
      data.set(name, value, TypeCodec.duration())
    }
  }

  implicit val localDateEncodeByName: EncodeByName[LocalDate] = new EncodeByName[LocalDate] {
    override def apply[B <: SettableData[B]](data: B, name: String, value: LocalDate): B = {
      data.setDate(name, value)
    }
  }

  implicit val localDateJEncodeByName: EncodeByName[LocalDateJ] = EncodeByName[LocalDate].contramap { a =>
    LocalDate.fromDaysSinceEpoch(a.toEpochDay.toInt)
  }

  object Ops {

    implicit class SettableDataOps[A <: SettableData[A]](val self: A) extends AnyVal {

      def encode[B](
        name: String,
        value: B,
      )(implicit
        encode: EncodeByName[B],
      ): A = {
        encode(self, name, value)
      }
    }
  }

  implicit class EncodeByNameOps[A](val self: EncodeByName[A]) extends AnyVal {

    def contramap[B](f: B => A): EncodeByName[B] = new EncodeByName[B] {
      override def apply[C <: SettableData[C]](data: C, name: String, value: B): C =
        self(data, name, f(value))
    }
  }
}

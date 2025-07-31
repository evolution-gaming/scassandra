package com.evolutiongaming.scassandra

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate as LocalDateJ}
import cats.Contravariant
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodecs
import com.datastax.oss.driver.api.core.cql.Bindable
import com.datastax.oss.driver.api.core.data.CqlDuration
import com.evolutiongaming.util.ToJava

trait EncodeByName[-A] {
  def bindToData[B <: Bindable[B]](data: B, name: String, value: A): B
}

object EncodeByName {

  implicit val contravariantEncodeByName: Contravariant[EncodeByName] = new Contravariant[EncodeByName] {
    def contramap[A, B](fa: EncodeByName[A])(f: B => A) = fa.contramap(f)
  }


  def apply[A](implicit encode: EncodeByName[A]): EncodeByName[A] = encode

  implicit def optEncodeByName[A](implicit encode: EncodeByName[A]): EncodeByName[Option[A]] = noneAsNull[A]


  def noneAsNull[A](implicit encode: EncodeByName[A]): EncodeByName[Option[A]] = new EncodeByName[Option[A]] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Option[A]): B = {
      value match {
        case Some(value) => encode.bindToData(data, name, value)
        case None        => data.setToNull(name)
      }
    }
  }

  def noneAsUnset[A](implicit encode: EncodeByName[A]): EncodeByName[Option[A]] = new EncodeByName[Option[A]] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Option[A]): B = {
      value match {
        case Some(value) => encode.bindToData(data, name, value)
        case None        => data
      }
    }
  }

  implicit val boolEncodeByName: EncodeByName[Boolean] = new EncodeByName[Boolean] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Boolean): B = data.setBool(name, value)
  }

  implicit val StrEncodeByName: EncodeByName[String] = new EncodeByName[String] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: String) = data.setString(name, value)
  }

  implicit val shortEncodeByName: EncodeByName[Short] = new EncodeByName[Short] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Short): B = data.setShort(name, value)
  }

  implicit val intEncodeByName: EncodeByName[Int] = new EncodeByName[Int] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Int): B = data.setInt(name, value)
  }

  implicit val longEncodeByName: EncodeByName[Long] = new EncodeByName[Long] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Long): B = data.setLong(name, value)
  }

  implicit val floatEncodeByName: EncodeByName[Float] = new EncodeByName[Float] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Float): B = data.setFloat(name, value)
  }

  implicit val doubleEncodeByName: EncodeByName[Double] = new EncodeByName[Double] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Double): B = data.setDouble(name, value)
  }


  implicit val instantEncodeByName: EncodeByName[Instant] = new EncodeByName[Instant] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Instant): B =
      data.setInstant(name, value)
  }


  implicit val bigDecimalEncodeByName: EncodeByName[BigDecimal] = new EncodeByName[BigDecimal] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: BigDecimal): B = {
      data.setBigDecimal(name, value.bigDecimal)
    }
  }


  implicit val setStrEncodeByName: EncodeByName[Set[String]] = new EncodeByName[Set[String]] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Set[String]): B = {
      val set = ToJava.from(value)
      data.setSet(name, set, classOf[String])
    }
  }

  implicit val bytesEncodeByName: EncodeByName[Array[Byte]] = new EncodeByName[Array[Byte]] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: Array[Byte]): B = {
      val bytes = ByteBuffer.wrap(value)
      data.setBytesUnsafe(name, bytes)
    }
  }

  implicit val durationEncodeByName: EncodeByName[CqlDuration] = new EncodeByName[CqlDuration] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: CqlDuration): B = {
      data.set(name, value, TypeCodecs.DURATION)
    }
  }

  implicit val localDateEncodeByName: EncodeByName[LocalDateJ] = new EncodeByName[LocalDateJ] {
    def bindToData[B <: Bindable[B]](data: B, name: String, value: LocalDateJ): B = {
      data.setLocalDate(name, value)
    }
  }

  object Ops {
    implicit class BindableOps[A <: Bindable[A]](private val self: A) extends AnyVal {
      def encode[B](name: String, value: B)(implicit encode: EncodeByName[B]): A = {
        encode.bindToData(self, name, value)
      }
    }
  }

  implicit class EncodeByNameOps[A](val self: EncodeByName[A]) extends AnyVal {
    def contramap[B](f: B => A): EncodeByName[B] = new EncodeByName[B] {
      def bindToData[C <: Bindable[C]](data: C, name: String, value: B): C =
        self.bindToData(data, name, f(value))
    }
  }
}

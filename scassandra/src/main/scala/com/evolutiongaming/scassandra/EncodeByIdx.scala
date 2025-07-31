package com.evolutiongaming.scassandra

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate as LocalDateJ}
import java.util.Date
import cats.Contravariant
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodecs
import com.datastax.oss.driver.api.core.cql.Bindable
import com.datastax.oss.driver.api.core.data.CqlDuration
import com.evolutiongaming.util.ToJava

trait EncodeByIdx[-A] {
  def apply[B <: Bindable[B]](data: B, idx: Int, value: A): B
}

object EncodeByIdx {
  implicit val contravariantEncodeByIdx: Contravariant[EncodeByIdx] = new Contravariant[EncodeByIdx] {
    def contramap[A, B](fa: EncodeByIdx[A])(f: B => A): EncodeByIdx[B] = fa.contramap(f)
  }

  def apply[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[A] = encode

  implicit def optEncodeByIdx[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[Option[A]] = new EncodeByIdx[Option[A]] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Option[A]): B = {
      value.fold {
        data.setToNull(idx)
      } { value =>
        encode(data, idx, value)
      }
    }
  }

  implicit val boolEncodeByIdx: EncodeByIdx[Boolean] = new EncodeByIdx[Boolean] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Boolean): B = data.setBoolean(idx, value)
  }


  implicit val strEncodeByIdx: EncodeByIdx[String] = new EncodeByIdx[String] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: String): B = data.setString(idx, value)
  }


  implicit val shortEncodeByIdx: EncodeByIdx[Short] = new EncodeByIdx[Short] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Short): B = data.setShort(idx, value)
  }


  implicit val intEncodeByIdx: EncodeByIdx[Int] = new EncodeByIdx[Int] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Int): B = data.setInt(idx, value)
  }


  implicit val longEncodeByIdx: EncodeByIdx[Long] = new EncodeByIdx[Long] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Long): B = data.setLong(idx, value)
  }


  implicit val floatEncodeByIdx: EncodeByIdx[Float] = new EncodeByIdx[Float] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Float): B = data.setFloat(idx, value)
  }


  implicit val DoubleEncodeByIdx: EncodeByIdx[Double] = new EncodeByIdx[Double] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Double): B = data.setDouble(idx, value)
  }


  implicit val instantEncodeByIdx: EncodeByIdx[Instant] = new EncodeByIdx[Instant] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Instant): B =
      data.setInstant(idx, value)
  }


  implicit val bigDecimalEncodeByIdx: EncodeByIdx[BigDecimal] = new EncodeByIdx[BigDecimal] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: BigDecimal): B = {
      data.setBigDecimal(idx, value.bigDecimal)
    }
  }


  implicit val setStrEncodeByIdx: EncodeByIdx[Set[String]] = new EncodeByIdx[Set[String]] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Set[String]): B = {
      val set = ToJava.from(value)
      data.setSet(idx, set, classOf[String])
    }
  }

  implicit val bytesEncodeByIdx: EncodeByIdx[Array[Byte]] = new EncodeByIdx[Array[Byte]] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: Array[Byte]): B = {
      val bytes = ByteBuffer.wrap(value)
      data.setBytesUnsafe(idx, bytes)
    }
  }

  implicit val durationEncodeByIdx: EncodeByIdx[CqlDuration] = new EncodeByIdx[CqlDuration] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: CqlDuration): B =
      data.set(idx, value, TypeCodecs.DURATION)
  }

  implicit val localDateEncodeByIdx: EncodeByIdx[LocalDateJ] = new EncodeByIdx[LocalDateJ] {
    def apply[B <: Bindable[B]](data: B, idx: Int, value: LocalDateJ): B =
      data.setLocalDate(idx, value)
  }

  object Ops {
    implicit class BindableOps[A <: Bindable[A]](private val self: A) extends AnyVal {
      def encodeAt[B](idx: Int, value: B)(implicit encode: EncodeByIdx[B]): A = {
        encode(self, idx, value)
      }
    }
  }

  implicit class EncodeByIdxOps[A](private val self: EncodeByIdx[A]) extends AnyVal {
    def contramap[B](f: B => A): EncodeByIdx[B] = new EncodeByIdx[B] {
      def apply[C <: Bindable[C]](data: C, idx: Int, value: B): C = self(data, idx, f(value))
    }
  }
}


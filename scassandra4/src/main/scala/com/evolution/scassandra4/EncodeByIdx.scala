package com.evolution.scassandra4

import cats.Contravariant
import com.datastax.oss.driver.api.core.data.{CqlDuration, SettableByIndex}

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters._

/** Note, that unlike driver 3's `SettableData`, driver 4's [[SettableByIndex]]
  * is immutable: the setters return a new instance, hence the result of
  * `apply` must be used rather than the passed in `data`.
  */
trait EncodeByIdx[-A] {

  def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: A): B
}

object EncodeByIdx {

  implicit val contravariantEncodeByIdx: Contravariant[EncodeByIdx] = new Contravariant[EncodeByIdx] {
    def contramap[A, B](fa: EncodeByIdx[A])(f: B => A) = fa.contramap(f)
  }


  def apply[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[A] = encode

  implicit def optEncodeByIdx[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[Option[A]] = new EncodeByIdx[Option[A]] {

    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Option[A]) = {
      value.fold {
        data.setToNull(idx)
      } { value =>
        encode(data, idx, value)
      }
    }
  }


  implicit val boolEncodeByIdx: EncodeByIdx[Boolean] = new EncodeByIdx[Boolean] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Boolean) = data.setBoolean(idx, value)
  }


  implicit val strEncodeByIdx: EncodeByIdx[String] = new EncodeByIdx[String] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: String) = data.setString(idx, value)
  }


  implicit val shortEncodeByIdx: EncodeByIdx[Short] = new EncodeByIdx[Short] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Short) = data.setShort(idx, value)
  }


  implicit val intEncodeByIdx: EncodeByIdx[Int] = new EncodeByIdx[Int] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Int) = data.setInt(idx, value)
  }


  implicit val longEncodeByIdx: EncodeByIdx[Long] = new EncodeByIdx[Long] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Long) = data.setLong(idx, value)
  }


  implicit val floatEncodeByIdx: EncodeByIdx[Float] = new EncodeByIdx[Float] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Float) = data.setFloat(idx, value)
  }


  implicit val doubleEncodeByIdx: EncodeByIdx[Double] = new EncodeByIdx[Double] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Double) = data.setDouble(idx, value)
  }


  implicit val instantEncodeByIdx: EncodeByIdx[Instant] = new EncodeByIdx[Instant] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Instant) = data.setInstant(idx, value)
  }


  implicit val bigDecimalEncodeByIdx: EncodeByIdx[BigDecimal] = new EncodeByIdx[BigDecimal] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: BigDecimal) = {
      data.setBigDecimal(idx, value.bigDecimal)
    }
  }


  implicit val setStrEncodeByIdx: EncodeByIdx[Set[String]] = new EncodeByIdx[Set[String]] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Set[String]) = {
      data.setSet(idx, value.asJava, classOf[String])
    }
  }

  implicit val bytesEncodeByIdx: EncodeByIdx[Array[Byte]] = new EncodeByIdx[Array[Byte]] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Array[Byte]) = {
      val bytes = ByteBuffer.wrap(value)
      data.setByteBuffer(idx, bytes)
    }
  }

  implicit val durationEncodeByIdx: EncodeByIdx[CqlDuration] = new EncodeByIdx[CqlDuration] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: CqlDuration) = {
      data.setCqlDuration(idx, value)
    }
  }

  implicit val localDateEncodeByIdx: EncodeByIdx[LocalDate] = new EncodeByIdx[LocalDate] {
    def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: LocalDate) = {
      data.setLocalDate(idx, value)
    }
  }

  object Ops {

    implicit class SettableByIndexOps[A <: SettableByIndex[A]](val self: A) extends AnyVal {

      def encodeAt[B](idx: Int, value: B)(implicit encode: EncodeByIdx[B]): A = {
        encode(self, idx, value)
      }
    }
  }


  implicit class EncodeByIdxOps[A](val self: EncodeByIdx[A]) extends AnyVal {

    def contramap[B](f: B => A): EncodeByIdx[B] = new EncodeByIdx[B] {
      def apply[C <: SettableByIndex[C]](data: C, idx: Int, value: B) = self(data, idx, f(value))
    }
  }
}

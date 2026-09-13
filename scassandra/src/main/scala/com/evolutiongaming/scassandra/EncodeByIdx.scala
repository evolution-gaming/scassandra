package com.evolutiongaming.scassandra

import cats.Contravariant
import com.datastax.oss.driver.api.core.data.{CqlDuration, SettableByIndex}

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters.*

trait EncodeByIdx[-A] {

  def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: A): B
}

object EncodeByIdx {

  implicit val contravariantEncodeByIdx: Contravariant[EncodeByIdx] = new Contravariant[EncodeByIdx] {
    override def contramap[A, B](fa: EncodeByIdx[A])(f: B => A): EncodeByIdx[B] = fa.contramap(f)
  }

  def apply[A](
    implicit
    encode: EncodeByIdx[A],
  ): EncodeByIdx[A] = encode

  implicit def optEncodeByIdx[A](
    implicit
    encode: EncodeByIdx[A],
  ): EncodeByIdx[Option[A]] = new EncodeByIdx[Option[A]] {

    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Option[A]): B = {
      value.fold {
        data.setToNull(idx)
      } { value =>
        encode(data, idx, value)
      }
    }
  }

  implicit val boolEncodeByIdx: EncodeByIdx[Boolean] = new EncodeByIdx[Boolean] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Boolean): B = {
      data.setBoolean(idx, value)
    }
  }

  implicit val strEncodeByIdx: EncodeByIdx[String] = new EncodeByIdx[String] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: String): B = {
      data.setString(idx, value)
    }
  }

  implicit val shortEncodeByIdx: EncodeByIdx[Short] = new EncodeByIdx[Short] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Short): B = {
      data.setShort(idx, value)
    }
  }

  implicit val intEncodeByIdx: EncodeByIdx[Int] = new EncodeByIdx[Int] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Int): B = {
      data.setInt(idx, value)
    }
  }

  implicit val longEncodeByIdx: EncodeByIdx[Long] = new EncodeByIdx[Long] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Long): B = {
      data.setLong(idx, value)
    }
  }

  implicit val floatEncodeByIdx: EncodeByIdx[Float] = new EncodeByIdx[Float] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Float): B = {
      data.setFloat(idx, value)
    }
  }

  implicit val DoubleEncodeByIdx: EncodeByIdx[Double] = new EncodeByIdx[Double] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Double): B = {
      data.setDouble(idx, value)
    }
  }

  implicit val instantEncodeByIdx: EncodeByIdx[Instant] = new EncodeByIdx[Instant] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Instant): B = {
      data.setInstant(idx, value)
    }
  }

  implicit val bigDecimalEncodeByIdx: EncodeByIdx[BigDecimal] = new EncodeByIdx[BigDecimal] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: BigDecimal): B = {
      data.setBigDecimal(idx, value.bigDecimal)
    }
  }

  implicit val setStrEncodeByIdx: EncodeByIdx[Set[String]] = new EncodeByIdx[Set[String]] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Set[String]): B = {
      data.setSet(idx, value.asJava, classOf[String])
    }
  }

  implicit val bytesEncodeByIdx: EncodeByIdx[Array[Byte]] = new EncodeByIdx[Array[Byte]] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: Array[Byte]): B = {
      data.setByteBuffer(idx, ByteBuffer.wrap(value))
    }
  }

  implicit val durationEncodeByIdx: EncodeByIdx[CqlDuration] = new EncodeByIdx[CqlDuration] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: CqlDuration): B = {
      data.setCqlDuration(idx, value)
    }
  }

  implicit val localDateEncodeByIdx: EncodeByIdx[LocalDate] = new EncodeByIdx[LocalDate] {
    override def apply[B <: SettableByIndex[B]](data: B, idx: Int, value: LocalDate): B = {
      data.setLocalDate(idx, value)
    }
  }

  object Ops {

    implicit class SettableByIndexOps[A <: SettableByIndex[A]](val self: A) extends AnyVal {

      def encodeAt[B](
        idx: Int,
        value: B,
      )(implicit
        encode: EncodeByIdx[B],
      ): A = {
        encode(self, idx, value)
      }
    }
  }

  implicit class EncodeByIdxOps[A](val self: EncodeByIdx[A]) extends AnyVal {

    def contramap[B](f: B => A): EncodeByIdx[B] = new EncodeByIdx[B] {
      override def apply[C <: SettableByIndex[C]](data: C, idx: Int, value: B): C = self(data, idx, f(value))
    }
  }
}

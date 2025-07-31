package com.evolutiongaming.scassandra

import java.time.{Instant, LocalDate as LocalDateJ}
import cats.Functor
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodecs
import com.datastax.oss.driver.api.core.data.{CqlDuration, GettableByIndex}
import com.evolutiongaming.util.ToScala

trait DecodeByIdx[A] {
  def apply(data: GettableByIndex, idx: Int): A
}

object DecodeByIdx {
  def apply[A](implicit decode: DecodeByIdx[A]): DecodeByIdx[A] = decode

  implicit val functorDecodeByIdx: Functor[DecodeByIdx] = new Functor[DecodeByIdx] {
    def map[A, B](fa: DecodeByIdx[A])(f: A => B): DecodeByIdx[B] = fa.map(f)
  }

  implicit def optDecodeByIdx[A](implicit decode: DecodeByIdx[A]): DecodeByIdx[Option[A]] =
    (data: GettableByIndex, idx: Int) => {
      if (data.isNull(idx)) None
      else Some(decode(data, idx))
    }

  implicit val boolDecodeByIdx: DecodeByIdx[Boolean] =
    (data: GettableByIndex, idx: Int) => data.getBoolean(idx)

  implicit val strDecodeByIdx: DecodeByIdx[String] =
    (data: GettableByIndex, idx: Int) => data.getString(idx)

  implicit val shortDecodeByIdx: DecodeByIdx[Short] =
    (data: GettableByIndex, idx: Int) => data.getShort(idx)

  implicit val intDecodeByIdx: DecodeByIdx[Int] =
    (data: GettableByIndex, idx: Int) => data.getInt(idx)

  implicit val longDecodeByIdx: DecodeByIdx[Long] =
    (data: GettableByIndex, idx: Int) => data.getLong(idx)

  implicit val floatDecodeByIdx: DecodeByIdx[Float] =
    (data: GettableByIndex, idx: Int) => data.getFloat(idx)

  implicit val doubleDecodeByIdx: DecodeByIdx[Double] =
    (data: GettableByIndex, idx: Int) => data.getDouble(idx)

  implicit val instantDecodeByIdx: DecodeByIdx[Instant] =
    (data: GettableByIndex, idx: Int) => data.getInstant(idx)

  implicit val bigDecimalDecodeByIdx: DecodeByIdx[BigDecimal] =
    (data: GettableByIndex, idx: Int) => BigDecimal(data.getBigDecimal(idx))


  implicit val setStrDecodeByIdx: DecodeByIdx[Set[String]] = {
    (data: GettableByIndex, idx: Int) => {
      val set = data.getSet(idx, classOf[String])
      ToScala.from(set).toSet
    }
  }

  implicit val bytesDecodeByIdx: DecodeByIdx[Array[Byte]] = {
    (data: GettableByIndex, idx: Int) => {
      val bytes = data.getBytesUnsafe(idx)
      bytes.array()
    }
  }

  implicit val durationDecodeByIdx: DecodeByIdx[CqlDuration] =
    (data: GettableByIndex, idx: Int) =>
      data.get(idx, TypeCodecs.DURATION)

  implicit val localDateDecodeByIdx: DecodeByIdx[LocalDateJ] = {
    (data: GettableByIndex, idx: Int) => data.getLocalDate(idx)
  }

  object Ops {
    implicit class GettableByIndexOps(private val self: GettableByIndex) extends AnyVal {
      def decodeAt[A](idx: Int)(implicit decode: DecodeByIdx[A]): A = decode(self, idx)
    }
  }

  implicit class DecodeByIdxOps[A](private val self: DecodeByIdx[A]) extends AnyVal {
    def map[B](f: A => B): DecodeByIdx[B] = (data: GettableByIndex, idx: Int) => f(self(data, idx))
  }
}
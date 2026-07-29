package com.evolutiongaming.scassandra

import cats.Functor
import com.datastax.driver.core.{Duration, GettableByIndexData, LocalDate, TypeCodec}

import java.time.{Instant, LocalDate as LocalDateJ}
import scala.jdk.CollectionConverters.*

trait DecodeByIdx[A] {

  def apply(data: GettableByIndexData, idx: Int): A
}

object DecodeByIdx {

  def apply[A](
    implicit
    decode: DecodeByIdx[A],
  ): DecodeByIdx[A] = decode

  implicit val functorDecodeByIdx: Functor[DecodeByIdx] = new Functor[DecodeByIdx] {
    override def map[A, B](fa: DecodeByIdx[A])(f: A => B): DecodeByIdx[B] = fa.map(f)
  }

  implicit def optDecodeByIdx[A](
    implicit
    decode: DecodeByIdx[A],
  ): DecodeByIdx[Option[A]] = (data: GettableByIndexData, idx: Int) => {
    if (data.isNull(idx)) None
    else Some(decode(data, idx))
  }

  implicit val boolDecodeByIdx: DecodeByIdx[Boolean] = (data: GettableByIndexData, idx: Int) => {
    data.getBool(idx)
  }

  implicit val strDecodeByIdx: DecodeByIdx[String] = (data: GettableByIndexData, idx: Int) => {
    data.getString(idx)
  }

  implicit val shortDecodeByIdx: DecodeByIdx[Short] = (data: GettableByIndexData, idx: Int) => {
    data.getShort(idx)
  }

  implicit val intDecodeByIdx: DecodeByIdx[Int] = (data: GettableByIndexData, idx: Int) => {
    data.getInt(idx)
  }

  implicit val longDecodeByIdx: DecodeByIdx[Long] = (data: GettableByIndexData, idx: Int) => {
    data.getLong(idx)
  }

  implicit val floatDecodeByIdx: DecodeByIdx[Float] = (data: GettableByIndexData, idx: Int) => {
    data.getFloat(idx)
  }

  implicit val doubleDecodeByIdx: DecodeByIdx[Double] = (data: GettableByIndexData, idx: Int) => {
    data.getDouble(idx)
  }

  implicit val instantDecodeByIdx: DecodeByIdx[Instant] = (data: GettableByIndexData, idx: Int) => {
    data.getTimestamp(idx).toInstant
  }

  implicit val bigDecimalDecodeByIdx: DecodeByIdx[BigDecimal] = (data: GettableByIndexData, idx: Int) => {
    BigDecimal(data.getDecimal(idx))
  }

  implicit val setStrDecodeByIdx: DecodeByIdx[Set[String]] = (data: GettableByIndexData, idx: Int) => {
    data.getSet(idx, classOf[String]).asScala.toSet
  }

  implicit val bytesDecodeByIdx: DecodeByIdx[Array[Byte]] = (data: GettableByIndexData, idx: Int) => {
    data.getBytes(idx).array()
  }

  implicit val durationDecodeByIdx: DecodeByIdx[Duration] = (data: GettableByIndexData, idx: Int) => {
    data.get(idx, TypeCodec.duration())
  }

  implicit val localDateDecodeByIdx: DecodeByIdx[LocalDate] = (data: GettableByIndexData, idx: Int) => {
    data.getDate(idx)
  }

  implicit val localDateJDecodeByIdx: DecodeByIdx[LocalDateJ] = DecodeByIdx[LocalDate].map { a =>
    LocalDateJ.ofEpochDay(a.getDaysSinceEpoch.toLong)
  }

  object Ops {

    implicit class GettableByIndexDataOps(val self: GettableByIndexData) extends AnyVal {

      def decodeAt[A](
        idx: Int,
      )(implicit
        decode: DecodeByIdx[A],
      ): A = decode(self, idx)
    }
  }

  implicit class DecodeByIdxOps[A](val self: DecodeByIdx[A]) extends AnyVal {

    def map[B](f: A => B): DecodeByIdx[B] = (data: GettableByIndexData, idx: Int) => f(self(data, idx))
  }
}

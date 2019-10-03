package com.evolutiongaming.scassandra

import java.nio.ByteBuffer
import java.time.Instant
import java.util.Date

import cats.Contravariant
import com.datastax.driver.core.SettableData
import com.evolutiongaming.util.ToJava

trait EncodeByIdx[-A] {

  def apply[B <: SettableData[B]](data: B, idx: Int, value: A): B
}

object EncodeByIdx {

  implicit val contravariantEncodeByIdx: Contravariant[EncodeByIdx] = new Contravariant[EncodeByIdx] {
    def contramap[A, B](fa: EncodeByIdx[A])(f: B => A) = fa.contramap(f)
  }


  def apply[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[A] = encode

  implicit def optEncodeByIdx[A](implicit encode: EncodeByIdx[A]): EncodeByIdx[Option[A]] = new EncodeByIdx[Option[A]] {

    def apply[B <: SettableData[B]](data: B, idx: Int, value: Option[A]) = {
      value.fold {
        data.setToNull(idx)
      } { value =>
        encode(data, idx, value)
      }
    }
  }


  implicit val boolEncodeByIdx: EncodeByIdx[Boolean] = new EncodeByIdx[Boolean] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Boolean) = data.setBool(idx, value)
  }


  implicit val strEncodeByIdx: EncodeByIdx[String] = new EncodeByIdx[String] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: String) = data.setString(idx, value)
  }


  implicit val shortEncodeByIdx: EncodeByIdx[Short] = new EncodeByIdx[Short] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Short) = data.setShort(idx, value)
  }


  implicit val intEncodeByIdx: EncodeByIdx[Int] = new EncodeByIdx[Int] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Int) = data.setInt(idx, value)
  }


  implicit val longEncodeByIdx: EncodeByIdx[Long] = new EncodeByIdx[Long] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Long) = data.setLong(idx, value)
  }


  implicit val floatEncodeByIdx: EncodeByIdx[Float] = new EncodeByIdx[Float] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Float) = data.setFloat(idx, value)
  }


  implicit val DoubleEncodeByIdx: EncodeByIdx[Double] = new EncodeByIdx[Double] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Double) = data.setDouble(idx, value)
  }


  implicit val instantEncodeByIdx: EncodeByIdx[Instant] = new EncodeByIdx[Instant] {

    def apply[B <: SettableData[B]](data: B, idx: Int, value: Instant) = {
      val timestamp = Date.from(value)
      data.setTimestamp(idx, timestamp)
    }
  }


  implicit val bigDecimalEncodeByIdx: EncodeByIdx[BigDecimal] = new EncodeByIdx[BigDecimal] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: BigDecimal) = {
      data.setDecimal(idx, value.bigDecimal)
    }
  }


  implicit val setStrEncodeByIdx: EncodeByIdx[Set[String]] = new EncodeByIdx[Set[String]] {
    def apply[B <: SettableData[B]](data: B, idx: Int, value: Set[String]) = {
      val set = ToJava.from(value)
      data.setSet(idx, set, classOf[String])
    }
  }

  implicit val bytesEncodeByIdx: EncodeByIdx[Array[Byte]] = new EncodeByIdx[Array[Byte]] {
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


  implicit class EncodeByIdxOps[A](val self: EncodeByIdx[A]) extends AnyVal {
    
    def contramap[B](f: B => A): EncodeByIdx[B] = new EncodeByIdx[B] {
      def apply[C <: SettableData[C]](data: C, idx: Int, value: B) = self(data, idx, f(value))
    }
  }
}


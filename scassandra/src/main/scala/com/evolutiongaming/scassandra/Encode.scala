package com.evolutiongaming.scassandra

import java.nio.ByteBuffer
import java.time.Instant
import java.util.Date

import com.datastax.driver.core.BoundStatement

import scala.collection.JavaConverters._

// TODO check performance of binding `by name`
// TODO cover with tests
// TODO add codecs for all supported types
trait Encode[-A] { self =>

  def apply(statement: BoundStatement, name: String, value: A): BoundStatement
  

  final def imap[B](f: B => A): Encode[B] = new Encode[B] {
    def apply(statement: BoundStatement, name: String, value: B) = self(statement, name, f(value))
  }
}

object Encode {

  def apply[A](implicit encode: Encode[A]): Encode[A] = encode

  implicit def opt[A](implicit encode: Encode[A]): Encode[Option[A]] = new Encode[Option[A]] {
    def apply(statement: BoundStatement, name: String, value: Option[A]) = {
      value match {
        case Some(value) => encode(statement, name, value)
        case None        => statement.setToNull(name)
      }
    }
  }

  
  implicit val BoolImpl: Encode[Boolean] = new Encode[Boolean] {
    def apply(statement: BoundStatement, name: String, value: Boolean) = statement.setBool(name, value)
  }

  implicit val BoolOptImpl: Encode[Option[Boolean]] = opt[Boolean]


  implicit val StrImpl: Encode[String] = new Encode[String] {
    def apply(statement: BoundStatement, name: String, value: String) = statement.setString(name, value)
  }

  implicit val StrOptImpl: Encode[Option[String]] = opt[String]


  implicit val ShortImpl: Encode[Short] = new Encode[Short] {
    def apply(statement: BoundStatement, name: String, value: Short) = statement.setShort(name, value)
  }

  implicit val ShortOptImpl: Encode[Option[Short]] = opt[Short]


  implicit val IntImpl: Encode[Int] = new Encode[Int] {
    def apply(statement: BoundStatement, name: String, value: Int) = statement.setInt(name, value)
  }

  implicit val IntOptImpl: Encode[Option[Int]] = opt[Int]


  implicit val LongImpl: Encode[Long] = new Encode[Long] {
    def apply(statement: BoundStatement, name: String, value: Long) = statement.setLong(name, value)
  }

  implicit val LongOptImpl: Encode[Option[Long]] = opt[Long]


  implicit val FloatImpl: Encode[Float] = new Encode[Float] {
    def apply(statement: BoundStatement, name: String, value: Float) = statement.setFloat(name, value)
  }

  implicit val FloatOptImpl: Encode[Option[Float]] = opt[Float]


  implicit val DoubleImpl: Encode[Double] = new Encode[Double] {
    def apply(statement: BoundStatement, name: String, value: Double) = statement.setDouble(name, value)
  }

  implicit val DoubleOptImpl: Encode[Option[Double]] = opt[Double]


  implicit val InstantImpl: Encode[Instant] = new Encode[Instant] {
    def apply(statement: BoundStatement, name: String, value: Instant) = {
      val timestamp = Date.from(value)
      statement.setTimestamp(name, timestamp)
    }
  }

  implicit val InstantOptImpl: Encode[Option[Instant]] = opt[Instant]


  implicit val BigDecimalImpl: Encode[BigDecimal] = new Encode[BigDecimal] {
    def apply(statement: BoundStatement, name: String, value: BigDecimal) = statement.setDecimal(name, value.bigDecimal)
  }

  implicit val BigDecimalOptImpl: Encode[Option[BigDecimal]] = opt[BigDecimal]


  implicit val SetStrImpl: Encode[Set[String]] = new Encode[Set[String]] {
    def apply(statement: BoundStatement, name: String, value: Set[String]) = {
      val set = value.asJava
      statement.setSet(name, set, classOf[String])
    }
  }

  implicit val BytesImpl: Encode[Array[Byte]] = new Encode[Array[Byte]] {
    def apply(statement: BoundStatement, name: String, value: Array[Byte]) = {
      val bytes = ByteBuffer.wrap(value)
      statement.setBytes(name, bytes)
    }
  }

  object Ops {
    
    implicit class BoundStatementOps(val self: BoundStatement) extends AnyVal {

      def encode[T](name: String, value: T)(implicit encode: Encode[T]): BoundStatement = {
        encode(self, name, value)
      }
    }
  }
}


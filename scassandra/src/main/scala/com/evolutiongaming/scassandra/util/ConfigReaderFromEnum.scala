package com.evolutiongaming.scassandra.util

import cats.syntax.all._
import pureconfig.error.{CannotParse, ConfigReaderFailures}
import pureconfig.{ConfigCursor, ConfigReader}

import scala.reflect.ClassTag

object ConfigReaderFromEnum {

  def apply[A <: Enum[A]](values: Array[A])(implicit tag: ClassTag[A]): ConfigReader[A] = {
    cursor: ConfigCursor => {

      def consistencyLevel(str: String) = {
        values
          .find { _.name equalsIgnoreCase str }
          .fold {
            val failure = CannotParse(s"Cannot parse ${ tag.runtimeClass } from $str", cursor.location)
            ConfigReaderFailures(failure).asLeft[A]
          } { _.asRight }
      }

      for {
        s <- cursor.asString
        r <- consistencyLevel(s)
      } yield r
    }
  }
}

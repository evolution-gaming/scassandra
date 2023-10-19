package com.evolutiongaming.scassandra.util

import cats.implicits._
import pureconfig.error.{CannotParse, ConfigReaderFailures}
import pureconfig.{ConfigCursor, ConfigReader}

import scala.reflect.ClassTag

/** Provides [[ConfigReader]] for Java enums.
  *
  * Example:
  * {{{
  * implicit val configReaderProtocolVersion: ConfigReader[ProtocolVersion] =
  *   ConfigReaderFromEnum(ProtocolVersion.values())
  * }}}
  */
object ConfigReaderFromEnum {

  /** Creates an instance of [[ConfigReader]] for specific Java enum.
    *
    * @param values List of all enum values, i.e. returned by `enum.values()` call.
    * @param tag [[ClassTag]] needed to report a class name on failure.
    */
  def apply[A <: Enum[A]](values: Array[A])(implicit tag: ClassTag[A]): ConfigReader[A] = {
    cursor: ConfigCursor => {

      def fromString(str: String) = {
        values
          .find { _.name equalsIgnoreCase str }
          .fold {
            val failure = CannotParse(s"Cannot parse ${ tag.runtimeClass } from $str", cursor.origin)
            ConfigReaderFailures(failure).asLeft[A]
          } { _.asRight }
      }

      for {
        s <- cursor.asString
        r <- fromString(s)
      } yield r
    }
  }
}

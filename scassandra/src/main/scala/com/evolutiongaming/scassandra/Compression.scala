package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.error.{CannotParse, ConfigReaderFailures}

/**
 * Protocol compression, see `advanced.protocol.compression` in the driver reference
 * configuration. `Lz4` and `Snappy` require the corresponding library on the classpath.
 */
sealed abstract class Compression(val name: String)

object Compression {

  case object None extends Compression("none")

  case object Lz4 extends Compression("lz4")

  case object Snappy extends Compression("snappy")

  val Values: List[Compression] = List(None, Lz4, Snappy)

  implicit val configReaderCompression: ConfigReader[Compression] = ConfigReader.fromCursor { cursor =>
    for {
      string <- cursor.asString
      compression <- Values
        .find(_.name.equalsIgnoreCase(string))
        .toRight(ConfigReaderFailures(CannotParse(s"Cannot parse Compression from $string", cursor.origin)))
    } yield compression
  }
}

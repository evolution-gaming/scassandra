package com.evolution.scassandra4

import cats.syntax.either._
import pureconfig.error.{CannotParse, ConfigReaderFailures}
import pureconfig.{ConfigCursor, ConfigReader}

/** Protocol compression, the counterpart of driver 3's `ProtocolOptions.Compression`.
  *
  * Driver 4 configures compression as a string (`advanced.protocol.compression`),
  * this type keeps the driver 3 era config schema (`none`/`lz4`/`snappy`) typed.
  */
sealed abstract class Compression(val name: String)

object Compression {

  case object None extends Compression("none")
  case object Lz4 extends Compression("lz4")
  case object Snappy extends Compression("snappy")

  val Values: List[Compression] = List(None, Lz4, Snappy)

  implicit val configReaderCompression: ConfigReader[Compression] = {
    (cursor: ConfigCursor) => {
      for {
        s <- cursor.asString
        r <- Values
          .find { _.name equalsIgnoreCase s }
          .fold {
            val failure = CannotParse(s"Cannot parse Compression from $s", cursor.origin)
            ConfigReaderFailures(failure).asLeft[Compression]
          } { _.asRight }
      } yield r
    }
  }
}

package com.evolutiongaming.scassandra

import cats.syntax.either._
import pureconfig.ConfigReader
import pureconfig.error.ExceptionThrown

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.11/manual/cloud/]]
  */
sealed trait CloudSecureConnectBundleConfig

object CloudSecureConnectBundleConfig {

  implicit val cloudSecureConnectBundleConfigReader: ConfigReader[CloudSecureConnectBundleConfig] =
    ConfigReader.fromCursor { c =>
      /*
        filtering out empty strings in case an unconditional env var is used in config and we want to be able
        to turn off the bundle loading with an empty string value
       */
      def getNonEmptyStr(prop: String): Option[String] =
        c.asObjectCursor.flatMap(_.atKey(prop)).flatMap(_.asString).toOption.filter(_.nonEmpty)

      val fileOpt = getNonEmptyStr("file")
      val urlOpt = getNonEmptyStr("url")

      (fileOpt, urlOpt) match {
        case (Some(filePath), _) => File(filePath).asRight
        case (_, Some(urlStr))   => Url(urlStr).asRight
        case (None, None)        => c.failed(ExceptionThrown(new IllegalArgumentException(
          "a non-empty string should be set for either file or url",
        )))
      }
    }

  /**
    * Cloud secure connect bundle file will be loaded from the filesystem
    */
  final case class File(path: String) extends CloudSecureConnectBundleConfig
  /**
    * Cloud secure connect bundle file will be loaded from an URL
    */
  final case class Url(url: String) extends CloudSecureConnectBundleConfig
}

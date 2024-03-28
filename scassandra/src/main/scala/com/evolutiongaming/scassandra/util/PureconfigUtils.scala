package com.evolutiongaming.scassandra.util

import pureconfig.error.ConfigReaderFailures
import pureconfig._

private[scassandra] object PureconfigSyntax {
  implicit final class ConfigObjectCursorSyntax(val objCur: ConfigObjectCursor) extends AnyVal {
    def getAt[A: ConfigReader](key: String): Either[ConfigReaderFailures, A] =
        objCur.atKey(key).flatMap(ConfigReader[A].from(_))

    def getAtOpt[A: ConfigReader](key: String): Either[ConfigReaderFailures, Option[A]] = 
        ConfigReader[Option[A]].from(objCur.atKeyOrUndefined(key))
  }
}
package com.evolutiongaming.scassandra

import pureconfig.ConfigReader

final case class Masked[A](value: A) {

  override def toString = "***"
}

object Masked {
  implicit def configReaderMasked[A: ConfigReader]: ConfigReader[Masked[A]] = ConfigReader[A].map { Masked(_) }
}

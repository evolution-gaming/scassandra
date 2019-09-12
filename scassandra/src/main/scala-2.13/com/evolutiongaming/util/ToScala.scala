package com.evolutiongaming.util

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ToScala {

  def from[T](collection: java.util.Collection[T]): Iterable[T] =
    collection.asScala

  def from[A, B](map: java.util.Map[A, B]): mutable.Map[A, B] =
    map.asScala
}
package com.evolutiongaming.util

import scala.collection.JavaConverters._
import scala.collection.mutable

object ToScala {

  def from[T](collection: java.util.Collection[T]): Iterable[T] =
    collection.asScala

  def from[A, B](map: java.util.Map[A, B]): mutable.Map[A, B] =
    map.asScala
}
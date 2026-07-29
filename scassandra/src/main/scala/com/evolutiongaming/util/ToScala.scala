package com.evolutiongaming.util

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

// TODO: [v6.0.0] remove
// leftover from Scala 2.12 cross-compilation support
@deprecated(since = "5.6.0", message = "will be removed in 6.0.0, use scala.jdk.CollectionConverters instead")
object ToScala {

  def from[T](collection: java.util.Collection[T]): Iterable[T] =
    collection.asScala

  def from[A, B](map: java.util.Map[A, B]): mutable.Map[A, B] =
    map.asScala
}

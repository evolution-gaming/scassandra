package com.evolutiongaming.util

import scala.jdk.CollectionConverters.*

// TODO: [v6.0.0] remove
// leftover from Scala 2.12 cross-compilation support
@deprecated(since = "5.6.0", message = "will be removed in 6.0.0, use scala.jdk.CollectionConverters instead")
object ToJava {

  def from[T](collection: List[T]): java.util.Collection[T] =
    collection.asJavaCollection

  def from[T](set: Set[T]): java.util.Set[T] =
    set.asJava

  def from[A, B](map: Map[A, B]): java.util.Map[A, B] =
    map.asJava
}

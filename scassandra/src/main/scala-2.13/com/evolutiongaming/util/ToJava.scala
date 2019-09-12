package com.evolutiongaming.util

import scala.jdk.CollectionConverters._

object ToJava {

  def from[T](collection: List[T]): java.util.Collection[T] =
    collection.asJavaCollection

  def from[T](set: Set[T]): java.util.Set[T] =
    set.asJava

  def from[A, B](map: Map[A, B]): java.util.Map[A, B] =
    map.asJava
}
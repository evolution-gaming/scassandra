package com.evolutiongaming.scassandra

object MockSupport {

  def notSupported[A]: A = sys.error("not supported")
}

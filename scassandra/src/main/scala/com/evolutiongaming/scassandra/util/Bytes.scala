package com.evolutiongaming.scassandra.util

import java.nio.ByteBuffer

private[scassandra] object Bytes {

  def toArray(buffer: ByteBuffer): Array[Byte] = {
    val bytes = new Array[Byte](buffer.remaining())
    buffer.duplicate().get(bytes)
    bytes
  }
}

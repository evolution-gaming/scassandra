package com.evolution.scassandra

import java.nio.ByteBuffer

private[scassandra] object Bytes {

  /**
   * Copies the remaining bytes, without changing the position of the buffer
   */
  def toArray(buffer: ByteBuffer): Array[Byte] = {
    val duplicate = buffer.duplicate()
    val bytes = new Array[Byte](duplicate.remaining())
    duplicate.get(bytes)
    bytes
  }
}

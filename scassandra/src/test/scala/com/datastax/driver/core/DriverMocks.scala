package com.datastax.driver.core

object CloseFutureMock {

  def apply(): CloseFuture = CloseFuture.immediateFuture()
}

object ColumnDefinitionsMock {

  val empty: ColumnDefinitions = ColumnDefinitions.EMPTY
}

object PreparedIdMock {

  val empty: PreparedId = {
    val metadata = new PreparedId.PreparedMetadata(MD5Digest.wrap(Array.empty[Byte]), ColumnDefinitions.EMPTY)
    new PreparedId(metadata, metadata, Array.empty[Int], ProtocolVersion.V4)
  }
}

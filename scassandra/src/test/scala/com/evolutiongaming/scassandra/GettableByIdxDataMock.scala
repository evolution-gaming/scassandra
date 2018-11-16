package com.evolutiongaming.scassandra

import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.time.Instant
import java.util.{Date, UUID}

import com.datastax.driver.core.{GettableByIndexData, LocalDate, TypeCodec}
import com.google.common.reflect.TypeToken

object GettableByIdxDataMock {

  def apply(): GettableByIndexData = {

    def notSupported() = sys.error("not support")

    new GettableByIndexData {
      def isNull(i: Int) = false
      def getBool(i: Int) = false
      def getByte(i: Int) = 0
      def getShort(i: Int) = 0
      def getInt(i: Int) = 0
      def getLong(i: Int) = 0l
      def getTimestamp(i: Int) = Date.from(Instant.ofEpochMilli(0))
      def getDate(i: Int) = LocalDate.fromDaysSinceEpoch(0)
      def getTime(i: Int) = 0l
      def getFloat(i: Int) = 0f
      def getDouble(i: Int) = 0d
      def getBytesUnsafe(i: Int) = ByteBuffer.allocate(0)
      def getBytes(i: Int) = ByteBuffer.allocate(0)
      def getString(i: Int) = i.toString
      def getVarint(i: Int) = BigInteger.ZERO
      def getDecimal(i: Int) = java.math.BigDecimal.ZERO
      def getUUID(i: Int) = UUID.randomUUID()
      def getInet(i: Int) = InetAddress.getLocalHost
      def getList[T](i: Int, elementsClass: Class[T]) = notSupported()
      def getList[T](i: Int, elementsType: TypeToken[T]) = notSupported()
      def getSet[T](i: Int, elementsClass: Class[T]) = notSupported()
      def getSet[T](i: Int, elementsType: TypeToken[T]) = notSupported()
      def getMap[K, V](i: Int, keysClass: Class[K], valuesClass: Class[V]) = notSupported()
      def getMap[K, V](i: Int, keysType: TypeToken[K], valuesType: TypeToken[V]) = notSupported()
      def getUDTValue(i: Int) = notSupported()
      def getTupleValue(i: Int) = notSupported()
      def getObject(i: Int) = new {}
      def get[T](i: Int, targetClass: Class[T]) = notSupported()
      def get[T](i: Int, targetType: TypeToken[T]) = notSupported()
      def get[T](i: Int, codec: TypeCodec[T]) = notSupported()
    }
  }
}


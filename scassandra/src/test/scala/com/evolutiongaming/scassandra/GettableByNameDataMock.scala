package com.evolutiongaming.scassandra

import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.time.Instant
import java.util.{Date, UUID}

import com.datastax.driver.core.{GettableByNameData, LocalDate, TypeCodec}
import com.google.common.reflect.TypeToken

object GettableByNameDataMock {

  def apply(): GettableByNameData = {

    def notSupported() = sys.error("not support")

    new GettableByNameData {
      def isNull(name: String) = false
      def getBool(name: String) = false
      def getByte(name: String) = 0
      def getShort(name: String) = 0
      def getInt(name: String) = 0
      def getLong(name: String) = 0l
      def getTimestamp(name: String) = Date.from(Instant.ofEpochMilli(0))
      def getDate(name: String) = LocalDate.fromDaysSinceEpoch(0)
      def getTime(name: String) = 0l
      def getFloat(name: String) = 0f
      def getDouble(name: String) = 0d
      def getBytesUnsafe(name: String) = ByteBuffer.allocate(0)
      def getBytes(name: String) = ByteBuffer.allocate(0)
      def getString(name: String) = name
      def getVarint(name: String) = BigInteger.ZERO
      def getDecimal(name: String) = java.math.BigDecimal.ZERO
      def getUUID(name: String) = UUID.randomUUID()
      def getInet(name: String) = InetAddress.getLocalHost
      def getList[A](name: String, elementsClass: Class[A]) = notSupported()
      def getList[A](name: String, elementsType: TypeToken[A]) = notSupported()
      def getSet[A](name: String, elementsClass: Class[A]) = notSupported()
      def getSet[A](name: String, elementsType: TypeToken[A]) = notSupported()
      def getMap[K, V](name: String, keysClass: Class[K], valuesClass: Class[V]) = notSupported()
      def getMap[K, V](name: String, keysType: TypeToken[K], valuesType: TypeToken[V]) = notSupported()
      def getUDTValue(name: String) = notSupported()
      def getTupleValue(name: String) = notSupported()
      def getObject(name: String) = new {}
      def get[A](name: String, targetClass: Class[A]) = notSupported()
      def get[A](name: String, targetType: TypeToken[A]) = notSupported()
      def get[A](name: String, codec: TypeCodec[A]) = notSupported()
    }
  }
}

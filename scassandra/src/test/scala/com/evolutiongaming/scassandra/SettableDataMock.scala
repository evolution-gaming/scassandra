package com.evolutiongaming.scassandra

import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.{Date, UUID, List => ListJ, Map => MapJ, Set => SetJ}

import com.datastax.driver.core._
import com.google.common.reflect.TypeToken

case class SettableDataMock(
  byName: Map[String, Any] = Map.empty,
  byIdx: Map[Int, Any] = Map.empty) extends SettableData[SettableDataMock] {

  def notSupported() = sys.error("not support")

  def setBool(i: Int, v: Boolean) = copy(byIdx = byIdx.updated(i, v))
  def setByte(i: Int, v: Byte) = copy(byIdx = byIdx.updated(i, v))
  def setShort(i: Int, v: Short) = copy(byIdx = byIdx.updated(i, v))
  def setInt(i: Int, v: Int) = copy(byIdx = byIdx.updated(i, v))
  def setLong(i: Int, v: Long) = copy(byIdx = byIdx.updated(i, v))
  def setTimestamp(i: Int, v: Date) = copy(byIdx = byIdx.updated(i, v))
  def setDate(i: Int, v: LocalDate) = copy(byIdx = byIdx.updated(i, v))
  def setTime(i: Int, v: Long) = copy(byIdx = byIdx.updated(i, v))
  def setFloat(i: Int, v: Float) = copy(byIdx = byIdx.updated(i, v))
  def setDouble(i: Int, v: Double) = copy(byIdx = byIdx.updated(i, v))
  def setString(i: Int, v: String) = copy(byIdx = byIdx.updated(i, v))
  def setBytes(i: Int, v: ByteBuffer) = copy(byIdx = byIdx.updated(i, v))
  def setBytesUnsafe(i: Int, v: ByteBuffer) = copy(byIdx = byIdx.updated(i, v))
  def setVarint(i: Int, v: BigInteger) = copy(byIdx = byIdx.updated(i, v))
  def setDecimal(i: Int, v: java.math.BigDecimal) = copy(byIdx = byIdx.updated(i, v))
  def setUUID(i: Int, v: UUID) = copy(byIdx = byIdx.updated(i, v))
  def setInet(i: Int, v: InetAddress) = copy(byIdx = byIdx.updated(i, v))
  def setList[E](i: Int, v: ListJ[E]) = copy(byIdx = byIdx.updated(i, v))
  def setList[E](i: Int, v: ListJ[E], elementsClass: Class[E]) = notSupported()
  def setList[E](i: Int, v: ListJ[E], elementsType: TypeToken[E]) = notSupported()
  def setMap[K, V](i: Int, v: MapJ[K, V]) = copy(byIdx = byIdx.updated(i, v))
  def setMap[K, V](i: Int, v: MapJ[K, V], keysClass: Class[K], valuesClass: Class[V]) = notSupported()
  def setMap[K, V](i: Int, v: MapJ[K, V], keysType: TypeToken[K], valuesType: TypeToken[V]) = notSupported()
  def setSet[E](i: Int, v: SetJ[E]) = notSupported()
  def setSet[E](i: Int, v: SetJ[E], elementsClass: Class[E]) = notSupported()
  def setSet[E](i: Int, v: SetJ[E], elementsType: TypeToken[E]) = notSupported()
  def setUDTValue(i: Int, v: UDTValue) = copy(byIdx = byIdx.updated(i, v))
  def setTupleValue(i: Int, v: TupleValue) = copy(byIdx = byIdx.updated(i, v))
  def setToNull(i: Int) = notSupported()
  def set[V](i: Int, v: V, targetClass: Class[V]) = notSupported()
  def set[V](i: Int, v: V, targetType: TypeToken[V]) = notSupported()
  def set[V](i: Int, v: V, codec: TypeCodec[V]) = notSupported()
  def setBool(name: String, v: Boolean) = copy(byName = byName.updated(name, v))
  def setByte(name: String, v: Byte) = copy(byName = byName.updated(name, v))
  def setShort(name: String, v: Short) = copy(byName = byName.updated(name, v))
  def setInt(name: String, v: Int) = copy(byName = byName.updated(name, v))
  def setLong(name: String, v: Long) = copy(byName = byName.updated(name, v))
  def setTimestamp(name: String, v: Date) = copy(byName = byName.updated(name, v))
  def setDate(name: String, v: LocalDate) = copy(byName = byName.updated(name, v))
  def setTime(name: String, v: Long) = copy(byName = byName.updated(name, v))
  def setFloat(name: String, v: Float) = copy(byName = byName.updated(name, v))
  def setDouble(name: String, v: Double) = copy(byName = byName.updated(name, v))
  def setString(name: String, v: String) = copy(byName = byName.updated(name, v))
  def setBytes(name: String, v: ByteBuffer) = copy(byName = byName.updated(name, v))
  def setBytesUnsafe(name: String, v: ByteBuffer) = copy(byName = byName.updated(name, v))
  def setVarint(name: String, v: BigInteger) = copy(byName = byName.updated(name, v))
  def setDecimal(name: String, v: java.math.BigDecimal) = copy(byName = byName.updated(name, v))
  def setUUID(name: String, v: UUID) = copy(byName = byName.updated(name, v))
  def setInet(name: String, v: InetAddress) = copy(byName = byName.updated(name, v))
  def setList[E](name: String, v: ListJ[E]) = copy(byName = byName.updated(name, v))
  def setList[E](name: String, v: ListJ[E], elementsClass: Class[E]) = copy(byName = byName.updated(name, v))
  def setList[E](name: String, v: ListJ[E], elementsType: TypeToken[E]) = copy(byName = byName.updated(name, v))
  def setMap[K, V](name: String, v: MapJ[K, V]) = copy(byName = byName.updated(name, v))
  def setMap[K, V](name: String, v: MapJ[K, V], keysClass: Class[K], valuesClass: Class[V]) = copy(byName = byName.updated(name, v))
  def setMap[K, V](name: String, v: MapJ[K, V], keysType: TypeToken[K], valuesType: TypeToken[V]) = copy(byName = byName.updated(name, v))
  def setSet[E](name: String, v: SetJ[E]) = copy(byName = byName.updated(name, v))
  def setSet[E](name: String, v: SetJ[E], elementsClass: Class[E]) = copy(byName = byName.updated(name, v))
  def setSet[E](name: String, v: SetJ[E], elementsType: TypeToken[E]) = copy(byName = byName.updated(name, v))
  def setUDTValue(name: String, v: UDTValue) = copy(byName = byName.updated(name, v))
  def setTupleValue(name: String, v: TupleValue) = copy(byName = byName.updated(name, v))
  def setToNull(name: String) = notSupported()
  def set[V](name: String, v: V, targetClass: Class[V]) = notSupported()
  def set[V](name: String, v: V, targetType: TypeToken[V]) = notSupported()
  def set[V](name: String, v: V, codec: TypeCodec[V]) = notSupported()
}

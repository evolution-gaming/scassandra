package com.evolutiongaming.scassandra

import java.math.{BigInteger, BigDecimal => BigDecimalJ}
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.{Date, UUID, List => ListJ, Map => MapJ, Set => SetJ}

import com.datastax.driver.core._
import com.google.common.reflect.TypeToken

case class DataMock(
  byName: Map[String, Any] = Map.empty,
  byIdx: Map[Int, Any] = Map.empty) extends SettableData[DataMock] with GettableData {

  private def notSupported() = sys.error("not support")

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
  def setList[E](i: Int, v: ListJ[E], elementsClass: Class[E]): DataMock = notSupported()
  def setList[E](i: Int, v: ListJ[E], elementsType: TypeToken[E]): DataMock = notSupported()
  def setMap[K, V](i: Int, v: MapJ[K, V]) = copy(byIdx = byIdx.updated(i, v))
  def setMap[K, V](i: Int, v: MapJ[K, V], keysClass: Class[K], valuesClass: Class[V]): DataMock = notSupported()
  def setMap[K, V](i: Int, v: MapJ[K, V], keysType: TypeToken[K], valuesType: TypeToken[V]): DataMock = notSupported()
  def setSet[E](i: Int, v: SetJ[E]) = copy(byIdx = byIdx.updated(i, v))
  def setSet[E](i: Int, v: SetJ[E], elementsClass: Class[E]) = copy(byIdx = byIdx.updated(i, v))
  def setSet[E](i: Int, v: SetJ[E], elementsType: TypeToken[E]) = copy(byIdx = byIdx.updated(i, v))
  def setUDTValue(i: Int, v: UDTValue) = copy(byIdx = byIdx.updated(i, v))
  def setTupleValue(i: Int, v: TupleValue) = copy(byIdx = byIdx.updated(i, v))
  def setToNull(i: Int) = copy(byIdx = byIdx - i)
  def set[V](i: Int, v: V, targetClass: Class[V]) = copy(byIdx = byIdx.updated(i, v))
  def set[V](i: Int, v: V, targetType: TypeToken[V]) = copy(byIdx = byIdx.updated(i, v))
  def set[V](i: Int, v: V, codec: TypeCodec[V]) = copy(byIdx = byIdx.updated(i, v))

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
  def setToNull(name: String) = copy(byName = byName - name)
  def set[V](name: String, v: V, targetClass: Class[V]) = copy(byName = byName.updated(name, v))
  def set[V](name: String, v: V, targetType: TypeToken[V]) = copy(byName = byName.updated(name, v))
  def set[V](name: String, v: V, codec: TypeCodec[V]) = copy(byName = byName.updated(name, v))

  def isNull(name: String) = !byName.contains(name)
  def getBool(name: String) = byName.getOrElse(name, null).asInstanceOf[Boolean]
  def getByte(name: String) = byName.getOrElse(name, null).asInstanceOf[Byte]
  def getShort(name: String) = byName.getOrElse(name, null).asInstanceOf[Short]
  def getInt(name: String) = byName.getOrElse(name, null).asInstanceOf[Int]
  def getLong(name: String) = byName.getOrElse(name, null).asInstanceOf[Long]
  def getTimestamp(name: String) = byName.getOrElse(name, null).asInstanceOf[Date]
  def getDate(name: String) = byName.getOrElse(name, null).asInstanceOf[LocalDate]
  def getTime(name: String) = byName.getOrElse(name, null).asInstanceOf[Long]
  def getFloat(name: String) = byName.getOrElse(name, null).asInstanceOf[Float]
  def getDouble(name: String) = byName.getOrElse(name, null).asInstanceOf[Double]
  def getBytesUnsafe(name: String) = byName.getOrElse(name, null).asInstanceOf[ByteBuffer]
  def getBytes(name: String) = byName.getOrElse(name, null).asInstanceOf[ByteBuffer]
  def getString(name: String) = byName.getOrElse(name, null).asInstanceOf[String]
  def getVarint(name: String) = byName.getOrElse(name, null).asInstanceOf[BigInteger]
  def getDecimal(name: String) = byName.getOrElse(name, null).asInstanceOf[BigDecimalJ]
  def getUUID(name: String) = byName.getOrElse(name, null).asInstanceOf[UUID]
  def getInet(name: String) = byName.getOrElse(name, null).asInstanceOf[InetAddress]
  def getList[T](name: String, elementsClass: Class[T]): ListJ[T] = notSupported()
  def getList[T](name: String, elementsType: TypeToken[T]): ListJ[T] = notSupported()
  def getSet[T](name: String, elementsClass: Class[T]) = byName.getOrElse(name, null).asInstanceOf[SetJ[T]]
  def getSet[T](name: String, elementsType: TypeToken[T]): SetJ[T] = notSupported()
  def getMap[K, V](name: String, keysClass: Class[K], valuesClass: Class[V]): MapJ[K, V] = notSupported()
  def getMap[K, V](name: String, keysType: TypeToken[K], valuesType: TypeToken[V]): MapJ[K, V] = notSupported()
  def getUDTValue(name: String): UDTValue = notSupported()
  def getTupleValue(name: String): TupleValue = notSupported()
  def getObject(name: String): Object = notSupported()
  def get[T](name: String, targetClass: Class[T]): T = notSupported()
  def get[T](name: String, targetType: TypeToken[T]): T = notSupported()
  def get[T](name: String, codec: TypeCodec[T]): T = byName.getOrElse(name, null).asInstanceOf[T]

  def isNull(i: Int) = !byIdx.contains(i)
  def getBool(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Boolean]
  def getByte(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Byte]
  def getShort(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Short]
  def getInt(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Int]
  def getLong(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Long]
  def getTimestamp(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Date]
  def getDate(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[LocalDate]
  def getTime(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Long]
  def getFloat(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Float]
  def getDouble(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Double]
  def getBytesUnsafe(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[ByteBuffer]
  def getBytes(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[ByteBuffer]
  def getString(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[String]
  def getVarint(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[BigInteger]
  def getDecimal(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[BigDecimalJ]
  def getUUID(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[UUID]
  def getInet(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[InetAddress]
  def getList[T](i: Int, elementsClass: Class[T]): ListJ[T] = notSupported()
  def getList[T](i: Int, elementsType: TypeToken[T]): ListJ[T] = notSupported()
  def getSet[T](i: Int, elementsClass: Class[T]) = byIdx.getOrElse(i, null).asInstanceOf[SetJ[T]]
  def getSet[T](i: Int, elementsType: TypeToken[T]) = byIdx.getOrElse(i, null).asInstanceOf[SetJ[T]]
  def getMap[K, V](i: Int, keysClass: Class[K], valuesClass: Class[V]): MapJ[K, V] = notSupported()
  def getMap[K, V](i: Int, keysType: TypeToken[K], valuesType: TypeToken[V]): MapJ[K, V] = notSupported()
  def getUDTValue(i: Int): UDTValue = notSupported()
  def getTupleValue(i: Int): TupleValue = notSupported()
  def getObject(i: Int): Object = notSupported()
  def get[T](i: Int, targetClass: Class[T]): T = notSupported()
  def get[T](i: Int, targetType: TypeToken[T]): T = notSupported()
  def get[T](i: Int, codec: TypeCodec[T]): T = byIdx.getOrElse(i, null).asInstanceOf[T]
}

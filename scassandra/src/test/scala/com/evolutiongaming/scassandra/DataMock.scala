package com.evolutiongaming.scassandra

import com.datastax.driver.core.*
import com.google.common.reflect.TypeToken

import java.math.{BigDecimal as BigDecimalJ, BigInteger}
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.{Date, List as ListJ, Map as MapJ, Set as SetJ, UUID}

// for com.google.common.reflect.TypeToken - used by the driver's SettableData API
//noinspection UnstableApiUsage
case class DataMock(
  byName: Map[String, Any] = Map.empty,
  byIdx: Map[Int, Any] = Map.empty,
) extends SettableData[DataMock] with GettableData {

  private def notSupported() = sys.error("not support")

  override def setBool(i: Int, v: Boolean): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setByte(i: Int, v: Byte): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setShort(i: Int, v: Short): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setInt(i: Int, v: Int): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setLong(i: Int, v: Long): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setTimestamp(i: Int, v: Date): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setDate(i: Int, v: LocalDate): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setTime(i: Int, v: Long): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setFloat(i: Int, v: Float): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setDouble(i: Int, v: Double): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setString(i: Int, v: String): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setBytes(i: Int, v: ByteBuffer): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setBytesUnsafe(i: Int, v: ByteBuffer): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setVarint(i: Int, v: BigInteger): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setDecimal(i: Int, v: java.math.BigDecimal): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setUUID(i: Int, v: UUID): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setInet(i: Int, v: InetAddress): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setList[E](i: Int, v: ListJ[E]): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setList[E](i: Int, v: ListJ[E], elementsClass: Class[E]): DataMock = notSupported()
  override def setList[E](i: Int, v: ListJ[E], elementsType: TypeToken[E]): DataMock = notSupported()
  override def setMap[K, V](i: Int, v: MapJ[K, V]): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setMap[K, V](
    i: Int,
    v: MapJ[K, V],
    keysClass: Class[K],
    valuesClass: Class[V],
  ): DataMock = notSupported()
  override def setMap[K, V](
    i: Int,
    v: MapJ[K, V],
    keysType: TypeToken[K],
    valuesType: TypeToken[V],
  ): DataMock = notSupported()
  override def setSet[E](i: Int, v: SetJ[E]): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setSet[E](i: Int, v: SetJ[E], elementsClass: Class[E]): DataMock =
    copy(byIdx = byIdx.updated(i, v))
  override def setSet[E](i: Int, v: SetJ[E], elementsType: TypeToken[E]): DataMock =
    copy(byIdx = byIdx.updated(i, v))
  override def setUDTValue(i: Int, v: UDTValue): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setTupleValue(i: Int, v: TupleValue): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def setToNull(i: Int): DataMock = copy(byIdx = byIdx - i)
  override def set[V](i: Int, v: V, targetClass: Class[V]): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def set[V](i: Int, v: V, targetType: TypeToken[V]): DataMock = copy(byIdx = byIdx.updated(i, v))
  override def set[V](i: Int, v: V, codec: TypeCodec[V]): DataMock = copy(byIdx = byIdx.updated(i, v))

  override def setBool(name: String, v: Boolean): DataMock = copy(byName = byName.updated(name, v))
  override def setByte(name: String, v: Byte): DataMock = copy(byName = byName.updated(name, v))
  override def setShort(name: String, v: Short): DataMock = copy(byName = byName.updated(name, v))
  override def setInt(name: String, v: Int): DataMock = copy(byName = byName.updated(name, v))
  override def setLong(name: String, v: Long): DataMock = copy(byName = byName.updated(name, v))
  override def setTimestamp(name: String, v: Date): DataMock = copy(byName = byName.updated(name, v))
  override def setDate(name: String, v: LocalDate): DataMock = copy(byName = byName.updated(name, v))
  override def setTime(name: String, v: Long): DataMock = copy(byName = byName.updated(name, v))
  override def setFloat(name: String, v: Float): DataMock = copy(byName = byName.updated(name, v))
  override def setDouble(name: String, v: Double): DataMock = copy(byName = byName.updated(name, v))
  override def setString(name: String, v: String): DataMock = copy(byName = byName.updated(name, v))
  override def setBytes(name: String, v: ByteBuffer): DataMock = copy(byName = byName.updated(name, v))
  override def setBytesUnsafe(name: String, v: ByteBuffer): DataMock = copy(byName = byName.updated(name, v))
  override def setVarint(name: String, v: BigInteger): DataMock = copy(byName = byName.updated(name, v))
  override def setDecimal(name: String, v: java.math.BigDecimal): DataMock =
    copy(byName = byName.updated(name, v))
  override def setUUID(name: String, v: UUID): DataMock = copy(byName = byName.updated(name, v))
  override def setInet(name: String, v: InetAddress): DataMock = copy(byName = byName.updated(name, v))
  override def setList[E](name: String, v: ListJ[E]): DataMock = copy(byName = byName.updated(name, v))
  override def setList[E](name: String, v: ListJ[E], elementsClass: Class[E]): DataMock =
    copy(byName = byName.updated(name, v))
  override def setList[E](name: String, v: ListJ[E], elementsType: TypeToken[E]): DataMock =
    copy(byName = byName.updated(name, v))
  override def setMap[K, V](name: String, v: MapJ[K, V]): DataMock = copy(byName = byName.updated(name, v))
  override def setMap[K, V](
    name: String,
    v: MapJ[K, V],
    keysClass: Class[K],
    valuesClass: Class[V],
  ): DataMock = copy(byName = byName.updated(name, v))
  override def setMap[K, V](
    name: String,
    v: MapJ[K, V],
    keysType: TypeToken[K],
    valuesType: TypeToken[V],
  ): DataMock = copy(byName = byName.updated(name, v))
  override def setSet[E](name: String, v: SetJ[E]): DataMock = copy(byName = byName.updated(name, v))
  override def setSet[E](name: String, v: SetJ[E], elementsClass: Class[E]): DataMock =
    copy(byName = byName.updated(name, v))
  override def setSet[E](name: String, v: SetJ[E], elementsType: TypeToken[E]): DataMock =
    copy(byName = byName.updated(name, v))
  override def setUDTValue(name: String, v: UDTValue): DataMock = copy(byName = byName.updated(name, v))
  override def setTupleValue(name: String, v: TupleValue): DataMock = copy(byName = byName.updated(name, v))
  override def setToNull(name: String): DataMock = copy(byName = byName - name)
  override def set[V](name: String, v: V, targetClass: Class[V]): DataMock =
    copy(byName = byName.updated(name, v))
  override def set[V](name: String, v: V, targetType: TypeToken[V]): DataMock =
    copy(byName = byName.updated(name, v))
  override def set[V](name: String, v: V, codec: TypeCodec[V]): DataMock =
    copy(byName = byName.updated(name, v))

  override def isNull(name: String): Boolean = !byName.contains(name)
  override def getBool(name: String): Boolean = byName.getOrElse(name, null).asInstanceOf[Boolean]
  override def getByte(name: String): Byte = byName.getOrElse(name, null).asInstanceOf[Byte]
  override def getShort(name: String): Short = byName.getOrElse(name, null).asInstanceOf[Short]
  override def getInt(name: String): Int = byName.getOrElse(name, null).asInstanceOf[Int]
  override def getLong(name: String): Long = byName.getOrElse(name, null).asInstanceOf[Long]
  override def getTimestamp(name: String): Date = byName.getOrElse(name, null).asInstanceOf[Date]
  override def getDate(name: String): LocalDate = byName.getOrElse(name, null).asInstanceOf[LocalDate]
  override def getTime(name: String): Long = byName.getOrElse(name, null).asInstanceOf[Long]
  override def getFloat(name: String): Float = byName.getOrElse(name, null).asInstanceOf[Float]
  override def getDouble(name: String): Double = byName.getOrElse(name, null).asInstanceOf[Double]
  override def getBytesUnsafe(name: String): ByteBuffer =
    byName.getOrElse(name, null).asInstanceOf[ByteBuffer]
  override def getBytes(name: String): ByteBuffer = byName.getOrElse(name, null).asInstanceOf[ByteBuffer]
  override def getString(name: String): String = byName.getOrElse(name, null).asInstanceOf[String]
  override def getVarint(name: String): BigInteger = byName.getOrElse(name, null).asInstanceOf[BigInteger]
  override def getDecimal(name: String): BigDecimalJ = byName.getOrElse(name, null).asInstanceOf[BigDecimalJ]
  override def getUUID(name: String): UUID = byName.getOrElse(name, null).asInstanceOf[UUID]
  override def getInet(name: String): InetAddress = byName.getOrElse(name, null).asInstanceOf[InetAddress]
  override def getList[T](name: String, elementsClass: Class[T]): ListJ[T] = notSupported()
  override def getList[T](name: String, elementsType: TypeToken[T]): ListJ[T] = notSupported()
  override def getSet[T](name: String, elementsClass: Class[T]): SetJ[T] =
    byName.getOrElse(name, null).asInstanceOf[SetJ[T]]
  override def getSet[T](name: String, elementsType: TypeToken[T]): SetJ[T] = notSupported()
  override def getMap[K, V](name: String, keysClass: Class[K], valuesClass: Class[V]): MapJ[K, V] =
    notSupported()
  override def getMap[K, V](name: String, keysType: TypeToken[K], valuesType: TypeToken[V]): MapJ[K, V] =
    notSupported()
  override def getUDTValue(name: String): UDTValue = notSupported()
  override def getTupleValue(name: String): TupleValue = notSupported()
  override def getObject(name: String): Object = notSupported()
  override def get[T](name: String, targetClass: Class[T]): T = notSupported()
  override def get[T](name: String, targetType: TypeToken[T]): T = notSupported()
  override def get[T](name: String, codec: TypeCodec[T]): T = byName.getOrElse(name, null).asInstanceOf[T]

  override def isNull(i: Int): Boolean = !byIdx.contains(i)
  override def getBool(i: Int): Boolean = byIdx.getOrElse(i, null).asInstanceOf[Boolean]
  override def getByte(i: Int): Byte = byIdx.getOrElse(i, null).asInstanceOf[Byte]
  override def getShort(i: Int): Short = byIdx.getOrElse(i, null).asInstanceOf[Short]
  override def getInt(i: Int): Int = byIdx.getOrElse(i, null).asInstanceOf[Int]
  override def getLong(i: Int): Long = byIdx.getOrElse(i, null).asInstanceOf[Long]
  override def getTimestamp(i: Int): Date = byIdx.getOrElse(i, null).asInstanceOf[Date]
  override def getDate(i: Int): LocalDate = byIdx.getOrElse(i, null).asInstanceOf[LocalDate]
  override def getTime(i: Int): Long = byIdx.getOrElse(i, null).asInstanceOf[Long]
  override def getFloat(i: Int): Float = byIdx.getOrElse(i, null).asInstanceOf[Float]
  override def getDouble(i: Int): Double = byIdx.getOrElse(i, null).asInstanceOf[Double]
  override def getBytesUnsafe(i: Int): ByteBuffer = byIdx.getOrElse(i, null).asInstanceOf[ByteBuffer]
  override def getBytes(i: Int): ByteBuffer = byIdx.getOrElse(i, null).asInstanceOf[ByteBuffer]
  override def getString(i: Int): String = byIdx.getOrElse(i, null).asInstanceOf[String]
  override def getVarint(i: Int): BigInteger = byIdx.getOrElse(i, null).asInstanceOf[BigInteger]
  override def getDecimal(i: Int): BigDecimalJ = byIdx.getOrElse(i, null).asInstanceOf[BigDecimalJ]
  override def getUUID(i: Int): UUID = byIdx.getOrElse(i, null).asInstanceOf[UUID]
  override def getInet(i: Int): InetAddress = byIdx.getOrElse(i, null).asInstanceOf[InetAddress]
  override def getList[T](i: Int, elementsClass: Class[T]): ListJ[T] = notSupported()
  override def getList[T](i: Int, elementsType: TypeToken[T]): ListJ[T] = notSupported()
  override def getSet[T](i: Int, elementsClass: Class[T]): SetJ[T] =
    byIdx.getOrElse(i, null).asInstanceOf[SetJ[T]]
  override def getSet[T](i: Int, elementsType: TypeToken[T]): SetJ[T] =
    byIdx.getOrElse(i, null).asInstanceOf[SetJ[T]]
  override def getMap[K, V](i: Int, keysClass: Class[K], valuesClass: Class[V]): MapJ[K, V] = notSupported()
  override def getMap[K, V](i: Int, keysType: TypeToken[K], valuesType: TypeToken[V]): MapJ[K, V] =
    notSupported()
  override def getUDTValue(i: Int): UDTValue = notSupported()
  override def getTupleValue(i: Int): TupleValue = notSupported()
  override def getObject(i: Int): Object = notSupported()
  override def get[T](i: Int, targetClass: Class[T]): T = notSupported()
  override def get[T](i: Int, targetType: TypeToken[T]): T = notSupported()
  override def get[T](i: Int, codec: TypeCodec[T]): T = byIdx.getOrElse(i, null).asInstanceOf[T]
}

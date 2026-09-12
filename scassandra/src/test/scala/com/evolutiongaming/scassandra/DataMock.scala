package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.ProtocolVersion
import com.datastax.oss.driver.api.core.`type`.DataType
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodec
import com.datastax.oss.driver.api.core.`type`.codec.registry.CodecRegistry
import com.datastax.oss.driver.api.core.data.{CqlDuration, GettableByName, SettableByName}
import com.evolutiongaming.scassandra.MockSupport.notSupported

import java.math.BigDecimal as BigDecimalJ
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate}
import java.util.{List as ListJ, Set as SetJ}

final case class DataMock(
  byName: Map[String, Any] = Map.empty,
  byIdx: Map[Int, Any] = Map.empty,
) extends SettableByName[DataMock] with GettableByName {

  private def idx[A](i: Int): A = byIdx.getOrElse(i, null).asInstanceOf[A]
  private def name[A](name: String): A = byName.getOrElse(name, null).asInstanceOf[A]
  private def setIdx(i: Int, v: Any): DataMock = copy(byIdx = byIdx.updated(i, v))
  private def setName(name: String, v: Any): DataMock = copy(byName = byName.updated(name, v))

  override def size(): Int = notSupported
  override def getType(i: Int): DataType = notSupported
  override def getType(name: String): DataType = notSupported
  override def firstIndexOf(name: String): Int = notSupported
  override def allIndicesOf(name: String): ListJ[Integer] = notSupported
  override def codecRegistry(): CodecRegistry = notSupported
  override def protocolVersion(): ProtocolVersion = notSupported
  override def getBytesUnsafe(i: Int): ByteBuffer = notSupported
  override def getBytesUnsafe(name: String): ByteBuffer = notSupported
  override def setBytesUnsafe(i: Int, v: ByteBuffer): DataMock = notSupported
  override def setBytesUnsafe(name: String, v: ByteBuffer): DataMock = notSupported

  override def isNull(i: Int): Boolean = !byIdx.contains(i)
  override def getBoolean(i: Int): Boolean = idx(i)
  override def getShort(i: Int): Short = idx(i)
  override def getInt(i: Int): Int = idx(i)
  override def getLong(i: Int): Long = idx(i)
  override def getFloat(i: Int): Float = idx(i)
  override def getDouble(i: Int): Double = idx(i)
  override def getString(i: Int): String = idx(i)
  override def getInstant(i: Int): Instant = idx(i)
  override def getLocalDate(i: Int): LocalDate = idx(i)
  override def getBigDecimal(i: Int): BigDecimalJ = idx(i)
  override def getByteBuffer(i: Int): ByteBuffer = idx(i)
  override def getCqlDuration(i: Int): CqlDuration = idx(i)
  override def getSet[E](i: Int, elementsClass: Class[E]): SetJ[E] = idx(i)
  override def get[V](i: Int, codec: TypeCodec[V]): V = idx(i)

  override def setToNull(i: Int): DataMock = copy(byIdx = byIdx - i)
  override def setBoolean(i: Int, v: Boolean): DataMock = setIdx(i, v)
  override def setShort(i: Int, v: Short): DataMock = setIdx(i, v)
  override def setInt(i: Int, v: Int): DataMock = setIdx(i, v)
  override def setLong(i: Int, v: Long): DataMock = setIdx(i, v)
  override def setFloat(i: Int, v: Float): DataMock = setIdx(i, v)
  override def setDouble(i: Int, v: Double): DataMock = setIdx(i, v)
  override def setString(i: Int, v: String): DataMock = setIdx(i, v)
  override def setInstant(i: Int, v: Instant): DataMock = setIdx(i, v)
  override def setLocalDate(i: Int, v: LocalDate): DataMock = setIdx(i, v)
  override def setBigDecimal(i: Int, v: BigDecimalJ): DataMock = setIdx(i, v)
  override def setByteBuffer(i: Int, v: ByteBuffer): DataMock = setIdx(i, v)
  override def setCqlDuration(i: Int, v: CqlDuration): DataMock = setIdx(i, v)
  override def setSet[E](i: Int, v: SetJ[E], elementsClass: Class[E]): DataMock = setIdx(i, v)
  override def set[V](i: Int, v: V, codec: TypeCodec[V]): DataMock = setIdx(i, v)

  override def isNull(name: String): Boolean = !byName.contains(name)
  override def getBoolean(name: String): Boolean = this.name(name)
  override def getShort(name: String): Short = this.name(name)
  override def getInt(name: String): Int = this.name(name)
  override def getLong(name: String): Long = this.name(name)
  override def getFloat(name: String): Float = this.name(name)
  override def getDouble(name: String): Double = this.name(name)
  override def getString(name: String): String = this.name(name)
  override def getInstant(name: String): Instant = this.name(name)
  override def getLocalDate(name: String): LocalDate = this.name(name)
  override def getBigDecimal(name: String): BigDecimalJ = this.name(name)
  override def getByteBuffer(name: String): ByteBuffer = this.name(name)
  override def getCqlDuration(name: String): CqlDuration = this.name(name)
  override def getSet[E](name: String, elementsClass: Class[E]): SetJ[E] = this.name(name)
  override def get[V](name: String, codec: TypeCodec[V]): V = this.name(name)

  override def setToNull(name: String): DataMock = copy(byName = byName - name)
  override def setBoolean(name: String, v: Boolean): DataMock = setName(name, v)
  override def setShort(name: String, v: Short): DataMock = setName(name, v)
  override def setInt(name: String, v: Int): DataMock = setName(name, v)
  override def setLong(name: String, v: Long): DataMock = setName(name, v)
  override def setFloat(name: String, v: Float): DataMock = setName(name, v)
  override def setDouble(name: String, v: Double): DataMock = setName(name, v)
  override def setString(name: String, v: String): DataMock = setName(name, v)
  override def setInstant(name: String, v: Instant): DataMock = setName(name, v)
  override def setLocalDate(name: String, v: LocalDate): DataMock = setName(name, v)
  override def setBigDecimal(name: String, v: BigDecimalJ): DataMock = setName(name, v)
  override def setByteBuffer(name: String, v: ByteBuffer): DataMock = setName(name, v)
  override def setCqlDuration(name: String, v: CqlDuration): DataMock = setName(name, v)
  override def setSet[E](name: String, v: SetJ[E], elementsClass: Class[E]): DataMock = setName(name, v)
  override def set[V](name: String, v: V, codec: TypeCodec[V]): DataMock = setName(name, v)
}

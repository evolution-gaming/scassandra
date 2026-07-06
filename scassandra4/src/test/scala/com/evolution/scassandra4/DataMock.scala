package com.evolution.scassandra4

import com.datastax.oss.driver.api.core.ProtocolVersion
import com.datastax.oss.driver.api.core.`type`.DataType
import com.datastax.oss.driver.api.core.`type`.codec.registry.CodecRegistry
import com.datastax.oss.driver.api.core.data.{CqlDuration, GettableByName, SettableByName}

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate}
import java.util.{Set => SetJ}

/** In-memory stand-in for a driver 4 row / bound statement.
  *
  * Driver 4 data interfaces implement the typed accessors as default methods
  * that serialize through codecs and delegate by-name calls to by-index ones
  * via `firstIndexOf`. This mock overrides the typed accessors used by the
  * scassandra4 codecs directly, storing plain values, so the abstract
  * byte-level primitives are left unsupported.
  */
case class DataMock(
  byName: Map[String, Any] = Map.empty,
  byIdx: Map[Int, Any] = Map.empty) extends GettableByName with SettableByName[DataMock] {

  private def notSupported() = sys.error("not supported")

  // abstract byte-level primitives, not used by this mock
  def size(): Int = notSupported()
  def getType(i: Int): DataType = notSupported()
  def firstIndexOf(name: String): Int = notSupported()
  def codecRegistry(): CodecRegistry = notSupported()
  def protocolVersion(): ProtocolVersion = notSupported()
  def getBytesUnsafe(i: Int): ByteBuffer = notSupported()
  def setBytesUnsafe(i: Int, v: ByteBuffer): DataMock = copy(byIdx = byIdx.updated(i, v))

  override def isNull(i: Int) = !byIdx.contains(i)
  override def getBoolean(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Boolean]
  override def getByte(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Byte]
  override def getShort(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Short]
  override def getInt(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Int]
  override def getLong(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Long]
  override def getFloat(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Float]
  override def getDouble(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Double]
  override def getString(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[String]
  override def getInstant(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[Instant]
  override def getLocalDate(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[LocalDate]
  override def getBigDecimal(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[java.math.BigDecimal]
  override def getByteBuffer(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[ByteBuffer]
  override def getCqlDuration(i: Int) = byIdx.getOrElse(i, null).asInstanceOf[CqlDuration]
  override def getSet[T](i: Int, elementsClass: Class[T]) = byIdx.getOrElse(i, null).asInstanceOf[SetJ[T]]

  override def isNull(name: String) = !byName.contains(name)
  override def getBoolean(name: String) = byName.getOrElse(name, null).asInstanceOf[Boolean]
  override def getByte(name: String) = byName.getOrElse(name, null).asInstanceOf[Byte]
  override def getShort(name: String) = byName.getOrElse(name, null).asInstanceOf[Short]
  override def getInt(name: String) = byName.getOrElse(name, null).asInstanceOf[Int]
  override def getLong(name: String) = byName.getOrElse(name, null).asInstanceOf[Long]
  override def getFloat(name: String) = byName.getOrElse(name, null).asInstanceOf[Float]
  override def getDouble(name: String) = byName.getOrElse(name, null).asInstanceOf[Double]
  override def getString(name: String) = byName.getOrElse(name, null).asInstanceOf[String]
  override def getInstant(name: String) = byName.getOrElse(name, null).asInstanceOf[Instant]
  override def getLocalDate(name: String) = byName.getOrElse(name, null).asInstanceOf[LocalDate]
  override def getBigDecimal(name: String) = byName.getOrElse(name, null).asInstanceOf[java.math.BigDecimal]
  override def getByteBuffer(name: String) = byName.getOrElse(name, null).asInstanceOf[ByteBuffer]
  override def getCqlDuration(name: String) = byName.getOrElse(name, null).asInstanceOf[CqlDuration]
  override def getSet[T](name: String, elementsClass: Class[T]) = byName.getOrElse(name, null).asInstanceOf[SetJ[T]]

  override def setToNull(i: Int) = copy(byIdx = byIdx - i)
  override def setBoolean(i: Int, v: Boolean) = copy(byIdx = byIdx.updated(i, v))
  override def setByte(i: Int, v: Byte) = copy(byIdx = byIdx.updated(i, v))
  override def setShort(i: Int, v: Short) = copy(byIdx = byIdx.updated(i, v))
  override def setInt(i: Int, v: Int) = copy(byIdx = byIdx.updated(i, v))
  override def setLong(i: Int, v: Long) = copy(byIdx = byIdx.updated(i, v))
  override def setFloat(i: Int, v: Float) = copy(byIdx = byIdx.updated(i, v))
  override def setDouble(i: Int, v: Double) = copy(byIdx = byIdx.updated(i, v))
  override def setString(i: Int, v: String) = copy(byIdx = byIdx.updated(i, v))
  override def setInstant(i: Int, v: Instant) = copy(byIdx = byIdx.updated(i, v))
  override def setLocalDate(i: Int, v: LocalDate) = copy(byIdx = byIdx.updated(i, v))
  override def setBigDecimal(i: Int, v: java.math.BigDecimal) = copy(byIdx = byIdx.updated(i, v))
  override def setByteBuffer(i: Int, v: ByteBuffer) = copy(byIdx = byIdx.updated(i, v))
  override def setCqlDuration(i: Int, v: CqlDuration) = copy(byIdx = byIdx.updated(i, v))
  override def setSet[T](i: Int, v: SetJ[T], elementsClass: Class[T]) = copy(byIdx = byIdx.updated(i, v))

  override def setToNull(name: String) = copy(byName = byName - name)
  override def setBoolean(name: String, v: Boolean) = copy(byName = byName.updated(name, v))
  override def setByte(name: String, v: Byte) = copy(byName = byName.updated(name, v))
  override def setShort(name: String, v: Short) = copy(byName = byName.updated(name, v))
  override def setInt(name: String, v: Int) = copy(byName = byName.updated(name, v))
  override def setLong(name: String, v: Long) = copy(byName = byName.updated(name, v))
  override def setFloat(name: String, v: Float) = copy(byName = byName.updated(name, v))
  override def setDouble(name: String, v: Double) = copy(byName = byName.updated(name, v))
  override def setString(name: String, v: String) = copy(byName = byName.updated(name, v))
  override def setInstant(name: String, v: Instant) = copy(byName = byName.updated(name, v))
  override def setLocalDate(name: String, v: LocalDate) = copy(byName = byName.updated(name, v))
  override def setBigDecimal(name: String, v: java.math.BigDecimal) = copy(byName = byName.updated(name, v))
  override def setByteBuffer(name: String, v: ByteBuffer) = copy(byName = byName.updated(name, v))
  override def setCqlDuration(name: String, v: CqlDuration) = copy(byName = byName.updated(name, v))
  override def setSet[T](name: String, v: SetJ[T], elementsClass: Class[T]) = copy(byName = byName.updated(name, v))
}

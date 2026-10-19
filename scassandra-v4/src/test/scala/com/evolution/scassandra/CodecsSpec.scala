package com.evolution.scassandra

import cats.syntax.all.*
import cats.{Contravariant, Functor}
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodecs
import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import com.datastax.oss.driver.api.core.`type`.{DataType, DataTypes}
import com.datastax.oss.driver.api.core.data.{CqlDuration, SettableByName, TupleValue, UdtValue}
import com.datastax.oss.driver.internal.core.`type`.UserDefinedTypeBuilder
import com.evolution.scassandra.CodecsSpec.*
import com.evolution.scassandra.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import java.time.{Instant, LocalDate}
import java.util.List as JavaList
import scala.jdk.CollectionConverters.*

class CodecsSpec extends AnyFunSuite with Matchers {

  test("encode and decode by name") {
    val data = udtValue()
      .encode("v_bool", bool)
      .encode("v_str", str)
      .encode("v_short", short)
      .encode("v_int", int)
      .encode("v_long", long)
      .encode("v_float", float)
      .encode("v_double", double)
      .encode("v_instant", instant)
      .encode("v_decimal", decimal)
      .encode("v_strs", strs)
      .encode("v_bytes", bytes)
      .encode("v_duration", duration)
      .encode("v_date", date)

    data.decode[Boolean]("v_bool") shouldEqual bool
    data.decode[String]("v_str") shouldEqual str
    data.decode[Short]("v_short") shouldEqual short
    data.decode[Int]("v_int") shouldEqual int
    data.decode[Long]("v_long") shouldEqual long
    data.decode[Float]("v_float") shouldEqual float
    data.decode[Double]("v_double") shouldEqual double
    data.decode[Instant]("v_instant") shouldEqual instant
    data.decode[BigDecimal]("v_decimal") shouldEqual decimal
    data.decode[Set[String]]("v_strs") shouldEqual strs
    data.decode[Array[Byte]]("v_bytes").toList shouldEqual bytes.toList
    data.decode[CqlDuration]("v_duration") shouldEqual duration
    data.decode[LocalDate]("v_date") shouldEqual date
  }

  test("encode and decode by index") {
    val data = tupleValue()
      .encodeAt(0, bool)
      .encodeAt(1, str)
      .encodeAt(2, short)
      .encodeAt(3, int)
      .encodeAt(4, long)
      .encodeAt(5, float)
      .encodeAt(6, double)
      .encodeAt(7, instant)
      .encodeAt(8, decimal)
      .encodeAt(9, strs)
      .encodeAt(10, bytes)
      .encodeAt(11, duration)
      .encodeAt(12, date)

    data.decodeAt[Boolean](0) shouldEqual bool
    data.decodeAt[String](1) shouldEqual str
    data.decodeAt[Short](2) shouldEqual short
    data.decodeAt[Int](3) shouldEqual int
    data.decodeAt[Long](4) shouldEqual long
    data.decodeAt[Float](5) shouldEqual float
    data.decodeAt[Double](6) shouldEqual double
    data.decodeAt[Instant](7) shouldEqual instant
    data.decodeAt[BigDecimal](8) shouldEqual decimal
    data.decodeAt[Set[String]](9) shouldEqual strs
    data.decodeAt[Array[Byte]](10).toList shouldEqual bytes.toList
    data.decodeAt[CqlDuration](11) shouldEqual duration
    data.decodeAt[LocalDate](12) shouldEqual date
  }

  test("encode None as null and decode null as None") {
    val byName = udtValue().encode("v_str", "str").encode("v_str", Option.empty[String])
    val byIdx = tupleValue().encodeAt(1, "str").encodeAt(1, Option.empty[String])

    byName.decode[Option[String]]("v_str") shouldEqual None
    byIdx.decodeAt[Option[String]](1) shouldEqual None
    udtValue().encode("v_int", 1.some).decode[Option[Int]]("v_int") shouldEqual Some(1)
    tupleValue().encodeAt(3, 1.some).decodeAt[Option[Int]](3) shouldEqual Some(1)
  }

  test("noneAsUnset leaves the value as is") {
    val byName = EncodeByName.noneAsUnset[String].apply(udtValue().encode("v_str", "str"), "v_str", None)
    val byIdx = EncodeByIdx.noneAsUnset[String].apply(tupleValue().encodeAt(1, "str"), 1, None)

    byName.decode[String]("v_str") shouldEqual "str"
    byIdx.decodeAt[String](1) shouldEqual "str"
  }

  test("encodeSome encodes Some and skips None") {
    def data = udtValue().encode("v_str", "str").encode("v_int", 1)

    data.encodeSome("v_str", "str2".some).decode[String]("v_str") shouldEqual "str2"
    data.encodeSome("v_str", Option.empty[String]).decode[String]("v_str") shouldEqual "str"
    data.encodeSome(Pair("str2", 2).some).decode[Pair] shouldEqual Pair("str2", 2)
    data.encodeSome(Option.empty[Pair]).decode[Pair] shouldEqual Pair("str", 1)
  }

  test("null is decoded as returned by the driver, unless decoded as Option") {
    val byName = udtValue()
    val byIdx = tupleValue()

    Option(byName.decode[String]("v_str")) shouldEqual None
    Option(byName.decode[Instant]("v_instant")) shouldEqual None
    Option(byName.decode[CqlDuration]("v_duration")) shouldEqual None
    Option(byName.decode[LocalDate]("v_date")) shouldEqual None
    byName.decode[Boolean]("v_bool") shouldEqual false
    byName.decode[Int]("v_int") shouldEqual 0
    byName.decode[Double]("v_double") shouldEqual 0.0
    byName.decode[Set[String]]("v_strs") shouldEqual Set.empty
    an[IllegalArgumentException] should be thrownBy byName.decode[BigDecimal]("v_decimal")
    a[NullPointerException] should be thrownBy byName.decode[Array[Byte]]("v_bytes")
    Option(byIdx.decodeAt[String](1)) shouldEqual None
    byIdx.decodeAt[Int](3) shouldEqual 0
  }

  test("decoding bytes copies the remaining bytes only") {
    val byName = udtValue().encode("v_bytes", bytes)
    val byIdx = tupleValue().encodeAt(10, bytes)
    val bufferByName = skipFirstByte(byName.getByteBuffer("v_bytes"))
    val bufferByIdx = skipFirstByte(byIdx.getByteBuffer(10))

    DecodeByName[Array[Byte]].apply(
      byName.setByteBuffer("v_bytes", bufferByName),
      "v_bytes",
    ).toList shouldEqual
      bytes.toList.drop(1)
    DecodeByIdx[Array[Byte]].apply(byIdx.setByteBuffer(10, bufferByIdx), 10).toList shouldEqual
      bytes.toList.drop(1)
  }

  test("codecs from the driver by name") {
    val decodedInt = DecodeByName.fromCodec(TypeCodecs.INT)
      .apply(EncodeByName.fromCodec(TypeCodecs.INT).apply(udtValue(), "v_int", Int.box(1)), "v_int")
    val decodedStr = DecodeByName.fromClass(classOf[String])
      .apply(EncodeByName.fromClass(classOf[String]).apply(udtValue(), "v_str", "str"), "v_str")
    val decodedList = DecodeByName.fromGenericType(listOfStr)
      .apply(EncodeByName.fromGenericType(listOfStr).apply(udtValue(), "v_list", strList), "v_list")

    decodedInt shouldEqual 1
    decodedStr shouldEqual "str"
    decodedList shouldEqual strList
  }

  test("codecs from the driver by index") {
    val decodedInt = DecodeByIdx.fromCodec(TypeCodecs.INT)
      .apply(EncodeByIdx.fromCodec(TypeCodecs.INT).apply(tupleValue(), 3, Int.box(1)), 3)
    val decodedStr = DecodeByIdx.fromClass(classOf[String])
      .apply(EncodeByIdx.fromClass(classOf[String]).apply(tupleValue(), 1, "str"), 1)
    val decodedList = DecodeByIdx.fromGenericType(listOfStr)
      .apply(EncodeByIdx.fromGenericType(listOfStr).apply(tupleValue(), 13, strList), 13)

    decodedInt shouldEqual 1
    decodedStr shouldEqual "str"
    decodedList shouldEqual strList
  }

  test("DecodeRow and EncodeRow") {
    udtValue().encode(Pair("str", 1)).decode[Pair] shouldEqual Pair("str", 1)
    DecodeRow[Int]("v_int").apply(udtValue().encode("v_int", 1)) shouldEqual 1
    EncodeRow[Int]("v_int").apply(udtValue(), 1).decode[Int]("v_int") shouldEqual 1
  }

  test("map and contramap") {
    DecodeByName[Int].map(_ + 1).apply(udtValue().encode("v_int", 1), "v_int") shouldEqual 2
    DecodeByIdx[Int].map(_ + 1).apply(tupleValue().encodeAt(3, 1), 3) shouldEqual 2
    DecodeRow[Int]("v_int").map(_ + 1).apply(udtValue().encode("v_int", 1)) shouldEqual 2
    EncodeByName[Int].contramap[String](_.toInt).apply(
      udtValue(),
      "v_int",
      "2",
    ).decode[Int]("v_int") shouldEqual 2
    EncodeByIdx[Int].contramap[String](_.toInt).apply(tupleValue(), 3, "2").decodeAt[Int](3) shouldEqual 2
    EncodeRow[Int]("v_int").contramap[String](_.toInt).apply(udtValue(), "2").decode[Int]("v_int") shouldEqual
      2
    UpdateByName[Int].contramap[String](_.toInt).apply(
      udtValue(),
      "v_int",
      "2",
    ).decode[Int]("v_int") shouldEqual 2
    UpdateByIdx[Int].contramap[String](_.toInt).apply(tupleValue(), 3, "2").decodeAt[Int](3) shouldEqual 2
    UpdateRow[Pair].contramap[String](Pair(_, 1)).apply(udtValue(), "str").decode[Pair] shouldEqual
      Pair("str", 1)
  }

  test("cats instances") {
    val decodeByName = Functor[DecodeByName].map(DecodeByName[Int])(_ + 1)
    val decodeByIdx = Functor[DecodeByIdx].map(DecodeByIdx[Int])(_ + 1)
    val decodeRow = Functor[DecodeRow].map(DecodeRow[Int]("v_int"))(_ + 1)
    val encodeByName = Contravariant[EncodeByName].contramap(EncodeByName[Int])((_: String).toInt)
    val encodeByIdx = Contravariant[EncodeByIdx].contramap(EncodeByIdx[Int])((_: String).toInt)
    val encodeRow = Contravariant[EncodeRow].contramap(EncodeRow[Int]("v_int"))((_: String).toInt)

    decodeByName(udtValue().encode("v_int", 1), "v_int") shouldEqual 2
    decodeByIdx(tupleValue().encodeAt(3, 1), 3) shouldEqual 2
    decodeRow(udtValue().encode("v_int", 1)) shouldEqual 2
    encodeByName(udtValue(), "v_int", "2").decode[Int]("v_int") shouldEqual 2
    encodeByIdx(tupleValue(), 3, "2").decodeAt[Int](3) shouldEqual 2
    encodeRow(udtValue(), "2").decode[Int]("v_int") shouldEqual 2
  }

  test("update syntax") {
    udtValue().update("v_int", 1).decode[Int]("v_int") shouldEqual 1
    tupleValue().updateAt(3, 1).decodeAt[Int](3) shouldEqual 1
    udtValue().update(Pair("str", 1)).decode[Pair] shouldEqual Pair("str", 1)
  }
}

object CodecsSpec {

  final case class Pair(str: String, int: Int)

  object Pair {

    implicit val encodeRowPair: EncodeRow[Pair] = new EncodeRow[Pair] {
      override def apply[B <: SettableByName[B]](data: B, pair: Pair): B = {
        data.encode("v_str", pair.str).encode("v_int", pair.int)
      }
    }

    implicit val decodeRowPair: DecodeRow[Pair] = { data =>
      Pair(data.decode[String]("v_str"), data.decode[Int]("v_int"))
    }
  }

  private val listOfStr: GenericType[JavaList[String]] = GenericType.listOf(classOf[String])

  private val strList: JavaList[String] = List("a", "b").asJava

  private def skipFirstByte(buffer: ByteBuffer): ByteBuffer = {
    val _ = buffer.get()
    buffer
  }

  private val columns: List[(String, DataType)] = List(
    "v_bool" -> DataTypes.BOOLEAN,
    "v_str" -> DataTypes.TEXT,
    "v_short" -> DataTypes.SMALLINT,
    "v_int" -> DataTypes.INT,
    "v_long" -> DataTypes.BIGINT,
    "v_float" -> DataTypes.FLOAT,
    "v_double" -> DataTypes.DOUBLE,
    "v_instant" -> DataTypes.TIMESTAMP,
    "v_decimal" -> DataTypes.DECIMAL,
    "v_strs" -> DataTypes.setOf(DataTypes.TEXT),
    "v_bytes" -> DataTypes.BLOB,
    "v_duration" -> DataTypes.DURATION,
    "v_date" -> DataTypes.DATE,
    "v_list" -> DataTypes.listOf(DataTypes.TEXT),
  )

  def udtValue(): UdtValue = {
    columns
      .foldLeft(new UserDefinedTypeBuilder("test_keyspace", "all_types")) {
        case (builder, (name, dataType)) => builder.withField(name, dataType)
      }
      .build()
      .newValue()
  }

  def tupleValue(): TupleValue = DataTypes.tupleOf(columns.map { case (_, dataType) => dataType }*).newValue()

  val bool = true
  val str = "str"
  val short: Short = 7
  val int = 42
  val long: Long = 1L << 40
  val float = 1.5f
  val double = 2.5
  val instant: Instant = Instant.parse("2020-01-02T03:04:05.678Z")
  val decimal: BigDecimal = BigDecimal("123.456")
  val strs: Set[String] = Set("a", "b")
  val bytes: Array[Byte] = Array[Byte](1, 2, 3)
  val duration: CqlDuration = CqlDuration.newInstance(1, 2, 3)
  val date: LocalDate = LocalDate.of(2020, 1, 2)
}

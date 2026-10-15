package com.evolution.scassandra

import cats.syntax.all.*
import com.datastax.oss.driver.api.core.`type`.codec.TypeCodecs
import com.datastax.oss.driver.api.core.`type`.reflect.GenericType
import com.datastax.oss.driver.api.core.`type`.{DataType, DataTypes}
import com.datastax.oss.driver.api.core.data.{CqlDuration, SettableByName, TupleValue, UdtValue}
import com.datastax.oss.driver.internal.core.`type`.UserDefinedTypeBuilder
import com.evolution.scassandra.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters.*

class CodecsSpec extends AnyFunSuite with Matchers {

  import CodecsSpec.*

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
    val encode = EncodeByName.noneAsUnset[String]
    val data = encode(udtValue().encode("v_str", "str"), "v_str", None)

    data.decode[String]("v_str") shouldEqual "str"
  }

  test("encodeSome skips None") {
    val data = udtValue().encode("v_str", "str").encodeSome("v_str", Option.empty[String])

    data.decode[String]("v_str") shouldEqual "str"
  }

  test("decoding bytes copies the remaining bytes only") {
    val data = udtValue().encode("v_bytes", bytes)
    val buffer = data.getByteBuffer("v_bytes")
    buffer.get()

    DecodeByName[Array[Byte]].apply(data.setByteBuffer("v_bytes", buffer), "v_bytes").toList shouldEqual
      bytes.toList.drop(1)
  }

  test("codecs from the driver") {
    val decodeInt = DecodeByName.fromCodec(TypeCodecs.INT)
    val encodeInt = EncodeByName.fromCodec(TypeCodecs.INT)
    val decodeStr = DecodeByIdx.fromClass(classOf[String])
    val encodeStr = EncodeByIdx.fromClass(classOf[String])
    val listOfStr = GenericType.listOf(classOf[String])
    val list = List("a", "b").asJava

    decodeInt(encodeInt(udtValue(), "v_int", Int.box(1)), "v_int") shouldEqual 1
    decodeStr(encodeStr(tupleValue(), 1, "str"), 1) shouldEqual "str"
    DecodeByName.fromGenericType(listOfStr)(
      EncodeByName.fromGenericType(listOfStr)(udtValue(), "v_list", list),
      "v_list",
    ) shouldEqual list
  }

  test("DecodeRow and EncodeRow") {
    implicit val encodeRow: EncodeRow[Pair] = new EncodeRow[Pair] {
      override def apply[B <: SettableByName[B]](data: B, pair: Pair): B = {
        data.encode("v_str", pair.str).encode("v_int", pair.int)
      }
    }
    implicit val decodeRow: DecodeRow[Pair] = { data =>
      Pair(data.decode[String]("v_str"), data.decode[Int]("v_int"))
    }

    udtValue().encode(Pair("str", 1)).decode[Pair] shouldEqual Pair("str", 1)
    DecodeRow[Int]("v_int").map(_ + 1).apply(udtValue().encode("v_int", 1)) shouldEqual 2
    EncodeRow[Int]("v_int").contramap[String](_.toInt).apply(udtValue(), "3").decode[Int]("v_int") shouldEqual
      3
  }

  test("update syntax") {
    udtValue().update("v_int", 1).decode[Int]("v_int") shouldEqual 1
    tupleValue().updateAt(3, 1).decodeAt[Int](3) shouldEqual 1
    UpdateByName[Int].contramap[String](_.toInt)(udtValue(), "v_int", "2").decode[Int]("v_int") shouldEqual 2
  }
}

object CodecsSpec {

  final case class Pair(str: String, int: Int)

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

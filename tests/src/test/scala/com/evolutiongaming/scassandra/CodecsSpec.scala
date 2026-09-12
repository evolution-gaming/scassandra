package com.evolutiongaming.scassandra

import cats.effect.IO
import com.datastax.driver.core.{Duration, LocalDate, Row}
import com.evolutiongaming.catshelper.CatsHelper.*
import com.evolutiongaming.scassandra.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, LocalDate as LocalDateJ}

class CodecsSpec extends AnyFunSuite with CassandraSuite with Matchers {

  import CodecsSpec.*

  private lazy val table = s"$keyspace.codecs"

  private lazy val columns = List(
    "v_bool",
    "v_str",
    "v_short",
    "v_int",
    "v_long",
    "v_float",
    "v_double",
    "v_instant",
    "v_decimal",
    "v_strs",
    "v_bytes",
    "v_duration",
    "v_date",
    "v_date_j",
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val create = s"""
      CREATE TABLE IF NOT EXISTS $table (
        id INT PRIMARY KEY,
        v_bool BOOLEAN,
        v_str TEXT,
        v_short SMALLINT,
        v_int INT,
        v_long BIGINT,
        v_float FLOAT,
        v_double DOUBLE,
        v_instant TIMESTAMP,
        v_decimal DECIMAL,
        v_strs SET<TEXT>,
        v_bytes BLOB,
        v_duration DURATION,
        v_date DATE,
        v_date_j DATE
      )
    """
    val program = for {
      _ <- session.execute(create)
      prepared <- session.prepare(
        s"INSERT INTO $table (id, ${ columns.mkString(", ") }) VALUES (?, ${ columns.map(_ => "?").mkString(", ") })",
      )
      bound = prepared
        .bind()
        .encode("id", FullRowId)
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
        .encode("v_date_j", dateJ)
      _ <- session.execute(bound)
      _ <- session.execute(s"INSERT INTO $table (id) VALUES (?)", Int.box(NullRowId))
    } yield ()
    program.toTry.get
  }

  private def select(id: Int): IO[Row] = {
    for {
      resultSet <-
        session.execute(s"SELECT id, ${ columns.mkString(", ") } FROM $table WHERE id = ?", Int.box(id))
    } yield resultSet.one()
  }

  test("decode by name") {
    val row = select(FullRowId).toTry.get
    row.decode[Boolean]("v_bool") shouldEqual bool
    row.decode[String]("v_str") shouldEqual str
    row.decode[Short]("v_short") shouldEqual short
    row.decode[Int]("v_int") shouldEqual int
    row.decode[Long]("v_long") shouldEqual long
    row.decode[Float]("v_float") shouldEqual float
    row.decode[Double]("v_double") shouldEqual double
    row.decode[Instant]("v_instant") shouldEqual instant
    row.decode[BigDecimal]("v_decimal") shouldEqual decimal
    row.decode[Set[String]]("v_strs") shouldEqual strs
    row.decode[Array[Byte]]("v_bytes").toList shouldEqual bytes.toList
    row.decode[Duration]("v_duration") shouldEqual duration
    row.decode[LocalDate]("v_date") shouldEqual date
    row.decode[LocalDateJ]("v_date_j") shouldEqual dateJ
  }

  test("decode by index") {
    val row = select(FullRowId).toTry.get
    row.decodeAt[Int](0) shouldEqual FullRowId
    row.decodeAt[Boolean](1) shouldEqual bool
    row.decodeAt[String](2) shouldEqual str
    row.decodeAt[Short](3) shouldEqual short
    row.decodeAt[Int](4) shouldEqual int
    row.decodeAt[Long](5) shouldEqual long
    row.decodeAt[Float](6) shouldEqual float
    row.decodeAt[Double](7) shouldEqual double
    row.decodeAt[Instant](8) shouldEqual instant
    row.decodeAt[BigDecimal](9) shouldEqual decimal
    row.decodeAt[Set[String]](10) shouldEqual strs
    row.decodeAt[Array[Byte]](11).toList shouldEqual bytes.toList
    row.decodeAt[Duration](12) shouldEqual duration
    row.decodeAt[LocalDate](13) shouldEqual date
    row.decodeAt[LocalDateJ](14) shouldEqual dateJ
  }

  test("decode present values as Some") {
    val row = select(FullRowId).toTry.get
    row.decode[Option[Boolean]]("v_bool") shouldEqual Some(bool)
    row.decode[Option[String]]("v_str") shouldEqual Some(str)
    row.decode[Option[Short]]("v_short") shouldEqual Some(short)
    row.decode[Option[Int]]("v_int") shouldEqual Some(int)
    row.decode[Option[Long]]("v_long") shouldEqual Some(long)
    row.decode[Option[Float]]("v_float") shouldEqual Some(float)
    row.decode[Option[Double]]("v_double") shouldEqual Some(double)
    row.decode[Option[Instant]]("v_instant") shouldEqual Some(instant)
    row.decode[Option[BigDecimal]]("v_decimal") shouldEqual Some(decimal)
    row.decode[Option[Set[String]]]("v_strs") shouldEqual Some(strs)
    row.decode[Option[Array[Byte]]]("v_bytes").map(_.toList) shouldEqual Some(bytes.toList)
    row.decode[Option[Duration]]("v_duration") shouldEqual Some(duration)
    row.decode[Option[LocalDate]]("v_date") shouldEqual Some(date)
    row.decode[Option[LocalDateJ]]("v_date_j") shouldEqual Some(dateJ)
  }

  test("decode null values as None") {
    val row = select(NullRowId).toTry.get
    row.decode[Option[Boolean]]("v_bool") shouldEqual None
    row.decode[Option[String]]("v_str") shouldEqual None
    row.decode[Option[Short]]("v_short") shouldEqual None
    row.decode[Option[Int]]("v_int") shouldEqual None
    row.decode[Option[Long]]("v_long") shouldEqual None
    row.decode[Option[Float]]("v_float") shouldEqual None
    row.decode[Option[Double]]("v_double") shouldEqual None
    row.decode[Option[Instant]]("v_instant") shouldEqual None
    row.decode[Option[BigDecimal]]("v_decimal") shouldEqual None
    row.decode[Option[Set[String]]]("v_strs") shouldEqual None
    row.decode[Option[Array[Byte]]]("v_bytes") shouldEqual None
    row.decode[Option[Duration]]("v_duration") shouldEqual None
    row.decode[Option[LocalDate]]("v_date") shouldEqual None
    row.decode[Option[LocalDateJ]]("v_date_j") shouldEqual None
    row.decodeAt[Option[String]](2) shouldEqual None
  }

  test("encode None as null") {
    val program = for {
      prepared <- session.prepare(s"INSERT INTO $table (id, v_str, v_int) VALUES (?, ?, ?)")
      bound = prepared
        .bind()
        .encode("id", OptionRowId)
        .encode("v_str", Option("str"))
        .encode("v_int", Option.empty[Int])
      _ <- session.execute(bound)
      row <- select(OptionRowId)
    } yield row
    val row = program.toTry.get
    row.decode[Option[String]]("v_str") shouldEqual Some("str")
    row.decode[Option[Int]]("v_int") shouldEqual None
  }
}

object CodecsSpec {

  val FullRowId = 1
  val NullRowId = 2
  val OptionRowId = 3

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
  val duration: Duration = Duration.newInstance(1, 2, 3)
  val date: LocalDate = LocalDate.fromYearMonthDay(2020, 1, 2)
  val dateJ: LocalDateJ = LocalDateJ.of(2020, 1, 2)
}

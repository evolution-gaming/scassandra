package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.data.CqlDuration
import com.evolutiongaming.scassandra.syntax.*
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate}

class EncodeDecodeByNameSpec extends AnyWordSpec with Matchers {

  def of[A](
    expected: A,
  )(implicit
    e: EncodeByName[A],
    d: DecodeByName[A],
    eOpt: EncodeByName[Option[A]],
    dOpt: DecodeByName[Option[A]],
  ): () => Assertion = {

    () =>
      {
        val data = DataMock()

        data
          .encode[A]("0", expected)
          .decode[A]("0") shouldEqual expected

        data
          .encode[Option[A]]("1", Some(expected))
          .decode[Option[A]]("1") shouldEqual Some(expected)

        data
          .encode[Option[A]]("2", None)
          .decode[Option[A]]("2") shouldEqual None
      }
  }

  "EncodeDecodeByName" should {

    for {
      (name, test) <- List(
        ("String", of("string")),
        ("Int", of(0)),
        ("Long", of(0L)),
        ("BigDecimal", of(BigDecimal(0))),
        ("Double", of(0d)),
        ("Float", of(0f)),
        ("Instant", of(Instant.now().truncatedTo(ChronoUnit.MILLIS))),
        ("Set", of(Set("str"))),
        ("Boolean", of(true)),
        ("Short", of(1.toShort)),
        ("Duration", of(CqlDuration.newInstance(1, 1, 1))),
        ("LocalDate", of(LocalDate.of(2019, 10, 4))),
      )
    } {
      s"encode & decode $name" in test()
    }

    "encode & decode Bytes" in {
      val bytes = Array[Byte](1, 2, 3)
      DataMock().encode("0", bytes).decode[Array[Byte]]("0").toList shouldEqual bytes.toList
      DataMock().encode("1", Some(bytes)).decode[Option[Array[Byte]]]("1").map(_.toList) shouldEqual
        Some(bytes.toList)
    }
  }
}

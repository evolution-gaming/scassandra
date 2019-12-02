package com.evolutiongaming.scassandra

import java.time.{Instant, LocalDate => LocalDateJ}
import java.time.temporal.ChronoUnit

import com.datastax.driver.core.{Duration, LocalDate}
import com.evolutiongaming.scassandra.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EncodeDecodeByIdxSpec extends AnyWordSpec with Matchers {

  def of[A](expected: A)(implicit
    e: EncodeByIdx[A],
    d: DecodeByIdx[A],
    eOpt: EncodeByIdx[Option[A]],
    dOpt: DecodeByIdx[Option[A]]) = {

    () => {
      val data = DataMock()

      data
        .encodeAt[A](0, expected)
        .decodeAt[A](0) shouldEqual expected

      data
        .encodeAt[Option[A]](1, Some(expected))
        .decodeAt[Option[A]](1) shouldEqual Some(expected)

      data
        .encodeAt[Option[A]](2, None)
        .decodeAt[Option[A]](2) shouldEqual None
    }
  }

  "EncodeDecodeByIdx" should {

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
        ("Duration", of(Duration.newInstance(1, 1, 1))),
        ("LocalDate", of(LocalDate.fromYearMonthDay(2019, 10, 4))),
        ("LocalDateJ", of(LocalDateJ.of(2019, 10, 4))))
    } {
      s"encode & decode $name" in test()
    }
  }
}

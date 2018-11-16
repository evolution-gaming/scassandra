package com.evolutiongaming.scassandra

import java.time.Instant

import com.evolutiongaming.scassandra.syntax._
import org.scalatest.{Matchers, WordSpec}

class EncodeDecodeByNameSpec extends WordSpec with Matchers {

  def of[A](expected: A)(implicit
    e: EncodeByName[A],
    d: DecodeByName[A],
    eOpt: EncodeByName[Option[A]],
    dOpt: DecodeByName[Option[A]]) = {

    () => {
      val data = DataMock()
      val actual = data
        .encode[A]("0", expected)
        .decode[A]("0")
      actual shouldEqual expected

      val actualOpt = data
        .encode[Option[A]]("1", Some(expected))
        .decode[Option[A]]("1")
      actualOpt shouldEqual Some(expected)
    }
  }

  "EncodeDecodeByName" should {

    for {
      (name, test) <- List(
        ("String", of("string")),
        ("Int", of(0)),
        ("Long", of(0l)),
        ("BigDecimal", of(BigDecimal(0))),
        ("Double", of(0d)),
        ("Float", of(0f)),
        ("Instant", of(Instant.now())),
        ("Set", of(Set("str"))))
    } {
      s"encode & decode $name" in test()
    }
  }
}

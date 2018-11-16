package com.evolutiongaming.scassandra

import java.time.Instant

import com.evolutiongaming.scassandra.syntax._
import org.scalatest.{Matchers, WordSpec}

class EncodeDecodeByIdxSpec extends WordSpec with Matchers {

  def of[A](expected: A)(implicit
    e: EncodeByIdx[A],
    d: DecodeByIdx[A],
    eOpt: EncodeByIdx[Option[A]],
    dOpt: DecodeByIdx[Option[A]]) = {

    () => {
      val data = DataMock()
      val actual = data
        .encodeAt[A](0, expected)
        .decodeAt[A](0)
      actual shouldEqual expected

      val actualOpt = data
        .encodeAt[Option[A]](1, Some(expected))
        .decodeAt[Option[A]](1)
      actualOpt shouldEqual Some(expected)
    }
  }

  "EncodeDecodeByIdx" should {

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

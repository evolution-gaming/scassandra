package com.evolution.scassandra

import cats.Contravariant
import com.evolution.scassandra.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ToCqlSpec extends AnyWordSpec with Matchers {

  private implicit val toCqlInt: ToCql[Int] = (value: Int) => value.toString

  "ToCql" should {

    "be summoned" in {
      ToCql[String].apply("value") shouldEqual "value"
      ToCql("value") shouldEqual "value"
    }

    "contramap" in {
      ToCql[String].contramap[Boolean](_.toString).apply(true) shouldEqual "true"
    }

    "have a Contravariant instance" in {
      Contravariant[ToCql].contramap(ToCql[String])((_: Boolean).toString).apply(false) shouldEqual "false"
    }

    "provide toCql via syntax" in {
      "value".toCql shouldEqual "value"
      2.toCql shouldEqual "2"
      TableName("keyspace", "table").toCql shouldEqual "keyspace.table"
    }
  }
}

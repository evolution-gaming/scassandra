package com.evolutiongaming.scassandra

import cats.Contravariant
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.annotation.nowarn

class ToCqlSpec extends AnyWordSpec with Matchers {

  private implicit val toCqlInt: ToCql[Int] = (a: Int) => a.toString

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

    "provide toCql via implicits" in {
      new ToCql.implicits.IdOpsToCql("value").toCql shouldEqual "value"
      new ToCql.implicits.IdOpsToCql(1).toCql shouldEqual "1"
    }

    "provide toCql via syntax" in {
      syntax.toCqlOps("value").toCql shouldEqual "value"
      syntax.toCqlOps(2).toCql shouldEqual "2"
    }

    "provide toCql via deprecated Ops" in {
      viaDeprecatedOps("value") shouldEqual "value"
    }
  }

  @nowarn("cat=deprecation")
  private def viaDeprecatedOps(a: String): String = new ToCql.Ops.IdOps(a).toCql
}

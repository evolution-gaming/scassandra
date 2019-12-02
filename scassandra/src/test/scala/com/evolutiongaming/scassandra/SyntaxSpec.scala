package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SyntaxSpec extends AnyWordSpec with Matchers {

  "Syntax" should {

    "decode by name" in {
      val data = DataMock(byName = Map(("key", "value")))
      data.decode[String]("key") shouldEqual "value"
    }

    "decode by idx" in {
      val data = DataMock(byIdx = Map((0, "value")))
      data.decodeAt[String](0) shouldEqual "value"
    }

    "encode by name" in {
      val data = DataMock()
      val data1 = data.encode("name", "str")
      data1.byName.get("name") shouldEqual Some("str")
    }

    "encode by idx" in {
      val data = DataMock()
      val data1 = data.encodeAt(0, "str")
      data1.byIdx.get(0) shouldEqual Some("str")
    }
  }
}

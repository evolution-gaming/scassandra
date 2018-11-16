package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.syntax._
import org.scalatest.{Matchers, WordSpec}

class SyntaxSpec extends WordSpec with Matchers {

  "Syntax" should {

    "decode by name" in {
      val data = GettableByNameDataMock()
      data.decode[String]("name") shouldEqual "name"
    }

    "decode by idx" in {
      val data = GettableByIdxDataMock()
      data.decode[String](0) shouldEqual "0"
    }

    "encode by name" in {
      val data = SettableDataMock()
      val data1 = data.encode("name", "str")
      data1.byName.get("name") shouldEqual Some("str")
    }

    "encode by idx" in {
      val data = SettableDataMock()
      val data1 = data.encode(0, "str")
      data1.byIdx.get(0) shouldEqual Some("str")
    }
  }
}

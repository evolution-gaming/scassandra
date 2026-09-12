package com.evolutiongaming.scassandra

import com.datastax.driver.core.ProtocolVersion
import com.evolutiongaming.scassandra.util.ConfigReaderFromEnum
import com.typesafe.config.ConfigValueFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.error.CannotParse

import scala.jdk.CollectionConverters.*

class ConfigReaderFromEnumSpec extends AnyFunSuite with Matchers {

  private val reader = ConfigReaderFromEnum(ProtocolVersion.values())

  test("read exact name") {
    reader.from(ConfigValueFactory.fromAnyRef("V4")) shouldEqual Right(ProtocolVersion.V4)
  }

  test("read ignoring case") {
    reader.from(ConfigValueFactory.fromAnyRef("v4")) shouldEqual Right(ProtocolVersion.V4)
    reader.from(ConfigValueFactory.fromAnyRef("v3")) shouldEqual Right(ProtocolVersion.V3)
  }

  test("fail on unknown name") {
    val failure = reader.from(ConfigValueFactory.fromAnyRef("V0")).swap.toOption.get.toList.head
    failure shouldBe a[CannotParse]
    failure.description should include("ProtocolVersion")
    failure.description should include("V0")
  }

  test("fail on non-string value") {
    reader.from(ConfigValueFactory.fromMap(Map("a" -> "b").asJava)).isLeft shouldEqual true
  }
}

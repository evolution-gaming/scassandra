package com.evolutiongaming.scassandra

import com.evolutiongaming.config.ConfigHelper.*
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ConfigHelpers.*
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource
import pureconfig.error.ConvertFailure
import pureconfig.module.cats.EmptyTraversableFound

class ConfigHelpersSpec extends AnyFunSuite with Matchers {

  test("nelFromConf reads a non-empty list") {
    ConfigFactory.parseString("""a = ["x", "y"]""").getOpt[Nel[String]]("a") shouldEqual Some(Nel("x", "y"))
  }

  test("nelFromConf returns None for a missing path") {
    ConfigFactory.empty().getOpt[Nel[String]]("a") shouldEqual None
  }

  test("nelFromConf fails on an empty list") {
    a[ConfigException.BadValue] should be thrownBy ConfigFactory.parseString("a = []").getOpt[Nel[String]]("a")
  }

  test("nelReader reads a non-empty list") {
    ConfigSource.string("""a = ["x", "y"]""").at("a").load[Nel[String]] shouldEqual Right(Nel("x", "y"))
  }

  test("nelReader fails on an empty list") {
    val failures = ConfigSource.string("a = []").at("a").load[Nel[String]].swap.toOption.get
    val reasons = failures.toList.collect { case ConvertFailure(reason, _, _) => reason }
    reasons should have size 1
    reasons.head shouldBe a[EmptyTraversableFound]
  }
}

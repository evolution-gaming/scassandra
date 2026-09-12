package com.evolutiongaming.scassandra

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class MaskedSpec extends AnyFunSuite with Matchers {

  test("toString hides the value") {
    Masked("secret").toString shouldEqual "***"
    s"${ Masked("secret") }" shouldEqual "***"
    Masked("secret").value shouldEqual "secret"
  }

  test("equality is by value") {
    Masked("a") shouldEqual Masked("a")
    Masked("a") should not equal Masked("b")
  }

  test("read from config") {
    ConfigSource.string("password = secret").at("password").load[Masked[String]] shouldEqual Right(Masked("secret"))
    ConfigSource.string("port = 1").at("port").load[Masked[Int]] shouldEqual Right(Masked(1))
  }
}

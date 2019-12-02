package com.evolutiongaming.scassandra

import cats.implicits._
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AuthenticationConfigSpec extends AnyFunSuite with Matchers {

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("authentication.conf"))
    val expected = AuthenticationConfig(
      username = "username",
      password = "password")
    ConfigSource.fromConfig(config).load[AuthenticationConfig] shouldEqual expected.asRight
  }
}
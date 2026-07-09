package com.evolutiongaming.scassandra

import cats.implicits.*
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

class AuthenticationConfigSpec extends AnyFunSuite with Matchers {

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("authentication.conf"))
    val expected = AuthenticationConfig(
      username = "username",
      password = "password",
    )
    ConfigSource.fromConfig(config).load[AuthenticationConfig] shouldEqual expected.asRight
  }
}

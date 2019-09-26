package com.evolutiongaming.scassandra

import cats.implicits._
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}
import pureconfig.ConfigSource

class AuthenticationConfigSpec extends FunSuite with Matchers {

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("authentication.conf"))
    val expected = AuthenticationConfig(
      username = "username",
      password = "password")
    ConfigSource.fromConfig(config).load[AuthenticationConfig] shouldEqual expected.asRight
  }
}
package com.evolutiongaming.scassandra

import cats.syntax.all._
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource

import scala.concurrent.duration._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ReconnectionConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[ReconnectionConfig] shouldEqual ReconnectionConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("reconnection.conf"))
    val expected = ReconnectionConfig(
      minDelay = 1.millis,
      maxDelay = 2.seconds)
    ConfigSource.fromConfig(config).load[ReconnectionConfig] shouldEqual expected.asRight
  }
}

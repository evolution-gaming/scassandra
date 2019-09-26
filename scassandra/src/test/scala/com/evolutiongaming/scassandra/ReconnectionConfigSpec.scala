package com.evolutiongaming.scassandra

import cats.implicits._
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}
import pureconfig.ConfigSource

import scala.concurrent.duration._

class ReconnectionConfigSpec extends FunSuite with Matchers {

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

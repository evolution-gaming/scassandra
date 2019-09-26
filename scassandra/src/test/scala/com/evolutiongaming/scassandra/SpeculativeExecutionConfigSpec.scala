package com.evolutiongaming.scassandra

import cats.implicits._
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}
import pureconfig.ConfigSource

import scala.concurrent.duration._

class SpeculativeExecutionConfigSpec extends FunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[SpeculativeExecutionConfig] shouldEqual SpeculativeExecutionConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("speculative-execution.conf"))
    val expected = SpeculativeExecutionConfig(
      delay = 1.millis,
      maxExecutions = 3)
    ConfigSource.fromConfig(config).load[SpeculativeExecutionConfig] shouldEqual expected.asRight
  }
}
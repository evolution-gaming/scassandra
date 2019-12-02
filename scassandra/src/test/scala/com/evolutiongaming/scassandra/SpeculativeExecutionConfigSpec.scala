package com.evolutiongaming.scassandra

import cats.implicits._
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource

import scala.concurrent.duration._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SpeculativeExecutionConfigSpec extends AnyFunSuite with Matchers {

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
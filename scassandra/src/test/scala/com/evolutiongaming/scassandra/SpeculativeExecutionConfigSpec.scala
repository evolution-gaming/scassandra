package com.evolutiongaming.scassandra

import cats.implicits.*
import com.datastax.driver.core.policies.ConstantSpeculativeExecutionPolicy
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.annotation.nowarn
import scala.concurrent.duration.*

class SpeculativeExecutionConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config)
      .load[SpeculativeExecutionConfig] shouldEqual SpeculativeExecutionConfig.Default.asRight
  }

  test("apply from config") {
    val config = ConfigFactory.parseURL(getClass.getResource("speculative-execution.conf"))
    val expected = SpeculativeExecutionConfig(
      delay = 1.millis,
      maxExecutions = 3,
    )
    ConfigSource.fromConfig(config).load[SpeculativeExecutionConfig] shouldEqual expected.asRight
  }

  test("asJava") {
    SpeculativeExecutionConfig(delay = 1.second, maxExecutions = 3).asJava shouldBe
      a[ConstantSpeculativeExecutionPolicy]
  }

  test("fromConfig falls back to default on invalid config") {
    val config = ConfigFactory.parseString("delay = nope")
    SpeculativeExecutionConfig.fromConfig(config, SpeculativeExecutionConfig.Default) shouldEqual
      SpeculativeExecutionConfig.Default
  }

  test("deprecated apply") {
    val config = ConfigFactory.parseString("max-executions = 5")
    (SpeculativeExecutionConfig(config): @nowarn("cat=deprecation")) shouldEqual
      SpeculativeExecutionConfig(maxExecutions = 5)
    (SpeculativeExecutionConfig(
      config,
      SpeculativeExecutionConfig.Default,
    ): @nowarn("cat=deprecation")) shouldEqual SpeculativeExecutionConfig(maxExecutions = 5)
  }
}

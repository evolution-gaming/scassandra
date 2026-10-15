package com.evolution.scassandra

import com.datastax.oss.driver.api.core.config.DefaultDriverOption.*
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile
import com.typesafe.config.ConfigFactory
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import java.time.Duration

class CassandraConfigSpec extends AnyFunSuite with Matchers with EitherValues {

  private def profileOf(config: CassandraConfig): DriverExecutionProfile = {
    config.configLoader.getInitialConfig.getDefaultProfile
  }

  test("Default has the defaults of the driver") {
    val profile = profileOf(CassandraConfig.Default)

    profile.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(2)
    profile.getInt(REQUEST_PAGE_SIZE) shouldEqual 5000
  }

  test("fromConfig reads the default path of the driver") {
    val config = ConfigFactory.parseString("datastax-java-driver.basic.request.timeout = 3s")
    val profile = profileOf(CassandraConfig.fromConfig(config).value)

    profile.getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(3)
    profile.getInt(REQUEST_PAGE_SIZE) shouldEqual 5000
  }

  test("fromConfig reads custom paths independently") {
    val config = ConfigFactory.parseString(
      """
        |cassandra1.basic.request.timeout = 3s
        |cassandra2.basic.request.timeout = 4s
        |""".stripMargin,
    )

    profileOf(CassandraConfig.fromConfig(config, "cassandra1").value)
      .getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(3)
    profileOf(CassandraConfig.fromConfig(config, "cassandra2").value)
      .getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(4)
  }

  test("fromConfig fails if the path is missing") {
    CassandraConfig.fromConfig(ConfigFactory.empty(), "cassandra").isLeft shouldEqual true
  }

  test("ConfigReader reads the driver configuration") {
    val config = ConfigFactory.parseString("a.b.basic.request.timeout = 3s")
    val result = ConfigSource.fromConfig(config).at("a.b").load[CassandraConfig].value

    profileOf(result).getDuration(REQUEST_TIMEOUT) shouldEqual Duration.ofSeconds(3)
  }

  test("configLoader does not support reloading") {
    CassandraConfig.Default.configLoader.supportsReloading() shouldEqual false
  }

  test("toString does not expose the configuration") {
    val config = ConfigFactory.parseString("advanced.auth-provider.password = secret")

    CassandraConfig(config).toString should not include "secret"
  }
}

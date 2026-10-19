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

  test("configLoader resolves the substitutions, also referring to the defaults of the driver") {
    val config = ConfigFactory.parseString(
      """
        |basic.request.page-size = 7
        |basic.request.page-size = ${?SCASSANDRA_UNDEFINED_VARIABLE}
        |advanced.connection.connect-timeout = ${basic.request.timeout}
        |""".stripMargin,
    )
    val profile = profileOf(CassandraConfig(config))

    profile.getInt(REQUEST_PAGE_SIZE) shouldEqual 7
    profile.getDuration(CONNECTION_CONNECT_TIMEOUT) shouldEqual Duration.ofSeconds(2)
  }

  test("configLoader does not support reloading") {
    CassandraConfig.Default.configLoader.supportsReloading() shouldEqual false
  }

  test("toString masks the secrets of the driver") {
    val config = ConfigFactory.parseString(
      """
        |advanced.auth-provider.password = value1
        |advanced.ssl-engine-factory.keystore-password = value2
        |advanced.ssl-engine-factory.truststore-password = value3
        |profiles.custom.advanced.auth-provider.password = value4
        |""".stripMargin,
    )
    val string = CassandraConfig(config).toString

    List("value1", "value2", "value3", "value4").foreach { value => string should not include value }
    string should include("\"password\":\"***\"")
  }

  test("toString masks the keys looking like secrets") {
    val config =
      ConfigFactory.parseString("custom { api-token = value1, client-secret = value2, credentials = value3 }")
    val string = CassandraConfig(config).toString

    List("value1", "value2", "value3").foreach { value => string should not include value }
  }

  test("toString prints the other options") {
    val config = ConfigFactory.parseString(
      """
        |basic.request.timeout = 5s
        |advanced.auth-provider.username = user
        |""".stripMargin,
    )

    CassandraConfig(config).toString shouldEqual
      """CassandraConfig(driver = {"advanced":{"auth-provider":{"username":"user"}},"basic":{"request":{"timeout":"5s"}}})"""
  }
}

package com.evolutiongaming.scassandra

import cats.implicits.*
import com.datastax.driver.core.ConsistencyLevel
import com.typesafe.config.ConfigFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

import scala.annotation.nowarn
import scala.concurrent.duration.*

class QueryConfigSpec extends AnyFunSuite with Matchers {

  test("apply from empty config") {
    val config = ConfigFactory.empty()
    ConfigSource.fromConfig(config).load[QueryConfig] shouldEqual QueryConfig.Default.asRight
  }

  test("apply from config") {
    val expected = QueryConfig(
      consistency = ConsistencyLevel.ALL,
      serialConsistency = ConsistencyLevel.QUORUM,
      fetchSize = 1,
      defaultIdempotence = true,
      maxPendingRefreshNodeListRequests = 2,
      maxPendingRefreshNodeRequests = 3,
      maxPendingRefreshSchemaRequests = 4,
      refreshNodeListInterval = 5.millis,
      refreshNodeInterval = 6.seconds,
      refreshSchemaInterval = 7.hours,
      metadata = false,
      rePrepareOnUp = false,
      prepareOnAllHosts = false,
    )
    val config = ConfigFactory.parseURL(getClass.getResource("query.conf"))
    ConfigSource.fromConfig(config).load[QueryConfig] shouldEqual expected.asRight
  }

  test("asJava") {
    val options = QueryConfig(
      consistency = ConsistencyLevel.ALL,
      serialConsistency = ConsistencyLevel.LOCAL_SERIAL,
      fetchSize = 1,
      defaultIdempotence = true,
      maxPendingRefreshNodeListRequests = 2,
      maxPendingRefreshNodeRequests = 3,
      maxPendingRefreshSchemaRequests = 4,
      refreshNodeListInterval = 5.seconds,
      refreshNodeInterval = 6.seconds,
      refreshSchemaInterval = 7.seconds,
      metadata = false,
      rePrepareOnUp = false,
      prepareOnAllHosts = false,
    ).asJava
    options.getConsistencyLevel shouldEqual ConsistencyLevel.ALL
    options.getSerialConsistencyLevel shouldEqual ConsistencyLevel.LOCAL_SERIAL
    options.getFetchSize shouldEqual 1
    options.getDefaultIdempotence shouldEqual true
    options.getMaxPendingRefreshNodeListRequests shouldEqual 2
    options.getMaxPendingRefreshNodeRequests shouldEqual 3
    options.getMaxPendingRefreshSchemaRequests shouldEqual 4
    options.getRefreshNodeListIntervalMillis shouldEqual 5000
    options.getRefreshNodeIntervalMillis shouldEqual 6000
    options.getRefreshSchemaIntervalMillis shouldEqual 7000
    options.isMetadataEnabled shouldEqual false
    options.isReprepareOnUp shouldEqual false
    options.isPrepareOnAllHosts shouldEqual false
  }

  test("fromConfig falls back to default on invalid config") {
    val config = ConfigFactory.parseString("fetch-size = nope")
    QueryConfig.fromConfig(config, QueryConfig.Default) shouldEqual QueryConfig.Default
  }

  test("deprecated apply") {
    val config = ConfigFactory.parseString("fetch-size = 7")
    (QueryConfig(config): @nowarn("cat=deprecation")) shouldEqual QueryConfig(fetchSize = 7)
    (QueryConfig(config, QueryConfig.Default): @nowarn("cat=deprecation")) shouldEqual
      QueryConfig(fetchSize = 7)
  }
}

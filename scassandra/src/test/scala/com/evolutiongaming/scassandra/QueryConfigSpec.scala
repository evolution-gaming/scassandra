package com.evolutiongaming.scassandra
import com.datastax.driver.core.ConsistencyLevel
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource

import scala.concurrent.duration._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

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
      prepareOnAllHosts = false)
    val config = ConfigFactory.parseURL(getClass.getResource("query.conf"))
    ConfigSource.fromConfig(config).load[QueryConfig] shouldEqual expected.asRight
  }
}
package com.evolutiongaming.scassandra

import com.datastax.driver.core.{ConsistencyLevel, QueryOptions}
import com.typesafe.config.Config
import pureconfig.ConfigSource

import scala.concurrent.duration._

/**
  * See [[https://docs.datastax.com/en/drivers/java/3.5/com/datastax/driver/core/QueryOptions.html]]
  */
final case class QueryConfig(
  consistency: ConsistencyLevel = ConsistencyLevel.LOCAL_ONE,
  serialConsistency: ConsistencyLevel = ConsistencyLevel.SERIAL,
  fetchSize: Int = 5000,
  defaultIdempotence: Boolean = false,
  maxPendingRefreshNodeListRequests: Int = 20,
  maxPendingRefreshNodeRequests: Int = 20,
  maxPendingRefreshSchemaRequests: Int = 20,
  refreshNodeListInterval: FiniteDuration = 1.second,
  refreshNodeInterval: FiniteDuration = 1.second,
  refreshSchemaInterval: FiniteDuration = 1.second,
  metadata: Boolean = true,
  rePrepareOnUp: Boolean = true,
  prepareOnAllHosts: Boolean = true) {

  def asJava: QueryOptions = {
    new QueryOptions()
      .setConsistencyLevel(consistency)
      .setSerialConsistencyLevel(serialConsistency)
      .setFetchSize(fetchSize)
      .setDefaultIdempotence(defaultIdempotence)
      .setMaxPendingRefreshNodeListRequests(maxPendingRefreshNodeListRequests)
      .setMaxPendingRefreshNodeRequests(maxPendingRefreshNodeRequests)
      .setMaxPendingRefreshSchemaRequests(maxPendingRefreshSchemaRequests)
      .setRefreshNodeListIntervalMillis(refreshNodeListInterval.toMillis.toInt)
      .setRefreshNodeIntervalMillis(refreshNodeInterval.toMillis.toInt)
      .setRefreshSchemaIntervalMillis(refreshSchemaInterval.toMillis.toInt)
      .setMetadataEnabled(metadata)
      .setReprepareOnUp(rePrepareOnUp)
      .setPrepareOnAllHosts(prepareOnAllHosts)
  }
}

object QueryConfig extends QueryConfigImplicits {

  val Default: QueryConfig = QueryConfig()

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): QueryConfig = apply(config, Default)

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config, default: => QueryConfig): QueryConfig = fromConfig(config, default)
  
  def fromConfig(config: Config, default: => QueryConfig): QueryConfig = {
    ConfigSource.fromConfig(config).load[QueryConfig] getOrElse default
  }
}

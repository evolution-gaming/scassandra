package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.{ConsistencyLevel, DefaultConsistencyLevel}
import com.typesafe.config.Config
import pureconfig.ConfigSource

import scala.concurrent.duration.*

/**
 * Request and metadata options, see `basic.request`, `advanced.metadata` and
 * `advanced.prepared-statements` in the driver reference configuration.
 */
final case class QueryConfig(
  consistency: ConsistencyLevel = DefaultConsistencyLevel.LOCAL_ONE,
  serialConsistency: ConsistencyLevel = DefaultConsistencyLevel.SERIAL,
  fetchSize: Int = 5000,
  defaultIdempotence: Boolean = false,
  maxPendingRefreshNodeListRequests: Int = 20,
  maxPendingRefreshSchemaRequests: Int = 20,
  refreshNodeListInterval: FiniteDuration = 1.second,
  refreshSchemaInterval: FiniteDuration = 1.second,
  metadata: Boolean = true,
  rePrepareOnUp: Boolean = true,
  prepareOnAllHosts: Boolean = true,
)

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

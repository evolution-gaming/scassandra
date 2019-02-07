package com.evolutiongaming.scassandra

import com.datastax.driver.core.{ConsistencyLevel, QueryOptions}
import com.evolutiongaming.config.ConfigHelper._
import com.typesafe.config.{Config, ConfigException}

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

object QueryConfig {

  val Default: QueryConfig = QueryConfig()

  private implicit val ConsistencyLevelFromConf = FromConf[ConsistencyLevel] { (conf, path) =>
    val str = conf.getString(path)
    val value = ConsistencyLevel.values().find { _.name equalsIgnoreCase str }
    value getOrElse {
      throw new ConfigException.BadValue(conf.origin(), path, s"Cannot parse ConsistencyLevel from $str")
    }
  }


  def apply(config: Config): QueryConfig = apply(config, Default)

  def apply(config: Config, default: => QueryConfig): QueryConfig = {

    def get[A: FromConf](name: String) = config.getOpt[A](name)

    QueryConfig(
      consistency = get[ConsistencyLevel]("consistency") getOrElse default.consistency,
      serialConsistency = get[ConsistencyLevel]("serial-consistency") getOrElse default.serialConsistency,
      fetchSize = get[Int]("fetch-size") getOrElse default.fetchSize,
      defaultIdempotence = get[Boolean]("default-idempotence") getOrElse default.defaultIdempotence,
      maxPendingRefreshNodeListRequests = get[Int]("max-pending-refresh-node-list-requests") getOrElse default.maxPendingRefreshNodeListRequests,
      maxPendingRefreshNodeRequests = get[Int]("max-pending-refresh-node-requests") getOrElse default.maxPendingRefreshNodeRequests,
      maxPendingRefreshSchemaRequests = get[Int]("max-pending-refresh-schema-requests") getOrElse default.maxPendingRefreshSchemaRequests,
      refreshNodeListInterval = get[FiniteDuration]("refresh-node-list-interval") getOrElse default.refreshNodeListInterval,
      refreshNodeInterval = get[FiniteDuration]("refresh-node-interval") getOrElse default.refreshNodeInterval,
      refreshSchemaInterval = get[FiniteDuration]("refresh-schema-interval") getOrElse default.refreshSchemaInterval,
      metadata = get[Boolean]("metadata") getOrElse default.metadata,
      rePrepareOnUp = get[Boolean]("re-prepare-on-up") getOrElse default.rePrepareOnUp,
      prepareOnAllHosts = get[Boolean]("prepare-on-all-hosts") getOrElse default.prepareOnAllHosts)
  }
}

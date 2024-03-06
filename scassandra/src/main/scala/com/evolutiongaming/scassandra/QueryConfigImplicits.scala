package com.evolutiongaming.scassandra

import com.datastax.driver.core.ConsistencyLevel
import com.evolutiongaming.scassandra.util.ConfigReaderFromEnum
import pureconfig.ConfigReader

import scala.concurrent.duration._

trait QueryConfigImplicits {
  implicit val configReaderConsistencyLevel: ConfigReader[ConsistencyLevel] = ConfigReaderFromEnum(ConsistencyLevel.values())

  implicit val configReaderQueryConfig: ConfigReader[QueryConfig] =
    ConfigReader.forProduct13[
      QueryConfig,
      Option[ConsistencyLevel],
      Option[ConsistencyLevel],
      Option[Int],
      Option[Boolean],
      Option[Int],
      Option[Int],
      Option[Int],
      Option[FiniteDuration],
      Option[FiniteDuration],
      Option[FiniteDuration],
      Option[Boolean],
      Option[Boolean],
      Option[Boolean]
    ](
      "consistency",
      "serial-consistency",
      "fetch-size",
      "default-idempotence",
      "max-pending-refresh-node-list-requests",
      "max-pending-refresh-node-requests",
      "max-pending-refresh-schema-requests",
      "refresh-node-list-interval",
      "refresh-node-interval",
      "refresh-schema-interval",
      "metadata",
      "re-prepare-on-up",
      "prepare-on-all-hosts"
    ) { (consistency, serialConsistency, fetchSize, defaultIdempotence, maxPendingRefreshNodeListRequests, maxPendingRefreshNodeRequests, maxPendingRefreshSchemaRequests, refreshNodeListInterval, refreshNodeInterval, refreshSchemaInterval, metadata, rePrepareOnUp, prepareOnAllHosts) =>
      val defaultConfig = QueryConfig()

      QueryConfig(
        consistency.getOrElse(defaultConfig.consistency),
        serialConsistency.getOrElse(defaultConfig.serialConsistency),
        fetchSize.getOrElse(defaultConfig.fetchSize),
        defaultIdempotence.getOrElse(defaultConfig.defaultIdempotence),
        maxPendingRefreshNodeListRequests.getOrElse(defaultConfig.maxPendingRefreshNodeListRequests),
        maxPendingRefreshNodeRequests.getOrElse(defaultConfig.maxPendingRefreshNodeRequests),
        maxPendingRefreshSchemaRequests.getOrElse(defaultConfig.maxPendingRefreshSchemaRequests),
        refreshNodeListInterval.getOrElse(defaultConfig.refreshNodeListInterval),
        refreshNodeInterval.getOrElse(defaultConfig.refreshNodeInterval),
        refreshSchemaInterval.getOrElse(defaultConfig.refreshSchemaInterval),
        metadata.getOrElse(defaultConfig.metadata),
        rePrepareOnUp.getOrElse(defaultConfig.rePrepareOnUp),
        prepareOnAllHosts.getOrElse(defaultConfig.prepareOnAllHosts)
      )
    }
  
}

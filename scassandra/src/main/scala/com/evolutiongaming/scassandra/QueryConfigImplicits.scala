package com.evolutiongaming.scassandra

import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.evolutiongaming.scassandra.util.ConfigReaderFromEnum
import com.evolutiongaming.scassandra.util.PureconfigSyntax.*
import pureconfig.ConfigReader

import scala.concurrent.duration.*

trait QueryConfigImplicits {
  implicit val configReaderConsistencyLevel: ConfigReader[ConsistencyLevel] = ConfigReaderFromEnum.forList(Vector(
    ConsistencyLevel.ANY,
    ConsistencyLevel.ONE,
    ConsistencyLevel.TWO,
    ConsistencyLevel.THREE,
    ConsistencyLevel.QUORUM,
    ConsistencyLevel.ALL,
    ConsistencyLevel.LOCAL_ONE,
    ConsistencyLevel.LOCAL_QUORUM,
    ConsistencyLevel.EACH_QUORUM,
    ConsistencyLevel.SERIAL,
    ConsistencyLevel.LOCAL_SERIAL,
  ), _.name)

  implicit val configReaderQueryConfig: ConfigReader[QueryConfig] = ConfigReader.fromCursor[QueryConfig] { cursor =>
    val defaultConfig = QueryConfig()

    for {
      objCur <- cursor.asObjectCursor
      consistency <- objCur.getAtOpt[ConsistencyLevel]("consistency").map(_.getOrElse(defaultConfig.consistency))
      serialConsistency <- objCur.getAtOpt[ConsistencyLevel]("serial-consistency").map(_.getOrElse(defaultConfig.serialConsistency))
      fetchSize <- objCur.getAtOpt[Int]("fetch-size").map(_.getOrElse(defaultConfig.fetchSize))
      defaultIdempotence <- objCur.getAtOpt[Boolean]("default-idempotence").map(_.getOrElse(defaultConfig.defaultIdempotence))
      maxPendingRefreshNodeListRequests <- objCur.getAtOpt[Int]("max-pending-refresh-node-list-requests").map(_.getOrElse(defaultConfig.maxPendingRefreshNodeListRequests))
      maxPendingRefreshNodeRequests <- objCur.getAtOpt[Int]("max-pending-refresh-node-requests").map(_.getOrElse(defaultConfig.maxPendingRefreshNodeRequests))
      maxPendingRefreshSchemaRequests <- objCur.getAtOpt[Int]("max-pending-refresh-schema-requests").map(_.getOrElse(defaultConfig.maxPendingRefreshSchemaRequests))
      refreshNodeListInterval <- objCur.getAtOpt[FiniteDuration]("refresh-node-list-interval").map(_.getOrElse(defaultConfig.refreshNodeListInterval))
      refreshNodeInterval <- objCur.getAtOpt[FiniteDuration]("refresh-node-interval").map(_.getOrElse(defaultConfig.refreshNodeInterval))
      refreshSchemaInterval <- objCur.getAtOpt[FiniteDuration]("refresh-schema-interval").map(_.getOrElse(defaultConfig.refreshSchemaInterval))
      metadata <- objCur.getAtOpt[Boolean]("metadata").map(_.getOrElse(defaultConfig.metadata))
      rePrepareOnUp <- objCur.getAtOpt[Boolean]("re-prepare-on-up").map(_.getOrElse(defaultConfig.rePrepareOnUp))
      prepareOnAllHosts <- objCur.getAtOpt[Boolean]("prepare-on-all-hosts").map(_.getOrElse(defaultConfig.prepareOnAllHosts))

    } yield QueryConfig(
      consistency = consistency,
      serialConsistency = serialConsistency,
      fetchSize = fetchSize,
      defaultIdempotence = defaultIdempotence,
      maxPendingRefreshNodeListRequests = maxPendingRefreshNodeListRequests,
      maxPendingRefreshNodeRequests = maxPendingRefreshNodeRequests,
      maxPendingRefreshSchemaRequests = maxPendingRefreshSchemaRequests,
      refreshNodeListInterval = refreshNodeListInterval,
      refreshNodeInterval = refreshNodeInterval,
      refreshSchemaInterval = refreshSchemaInterval,
      metadata = metadata,
      rePrepareOnUp = rePrepareOnUp,
      prepareOnAllHosts = prepareOnAllHosts
    )
  }
  
}
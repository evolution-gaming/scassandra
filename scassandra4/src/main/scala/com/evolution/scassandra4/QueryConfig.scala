package com.evolution.scassandra4

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel
import com.evolution.scassandra4.util.{ConfigReaderFromEnum, PureconfigSyntax}
import PureconfigSyntax._
import pureconfig.ConfigReader

import scala.concurrent.duration._

/** Query options, keeping the driver 3 era schema.
  *
  * See [[CreateDriverConfigLoader]] for the translation. Two fields have no
  * driver 4 counterpart and are ignored: `maxPendingRefreshNodeRequests` and
  * `refreshNodeInterval` (driver 4 debounces single-node and node-list
  * topology events together).
  */
final case class QueryConfig(
  consistency: DefaultConsistencyLevel = DefaultConsistencyLevel.LOCAL_ONE,
  serialConsistency: DefaultConsistencyLevel = DefaultConsistencyLevel.SERIAL,
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
  prepareOnAllHosts: Boolean = true)

object QueryConfig {

  val Default: QueryConfig = QueryConfig()

  implicit val configReaderConsistencyLevel: ConfigReader[DefaultConsistencyLevel] =
    ConfigReaderFromEnum(DefaultConsistencyLevel.values())

  implicit val configReaderQueryConfig: ConfigReader[QueryConfig] = ConfigReader.fromCursor[QueryConfig] { cursor =>
    val defaultConfig = QueryConfig()

    for {
      objCur <- cursor.asObjectCursor
      consistency <- objCur.getAtOpt[DefaultConsistencyLevel]("consistency").map(_.getOrElse(defaultConfig.consistency))
      serialConsistency <- objCur.getAtOpt[DefaultConsistencyLevel]("serial-consistency").map(_.getOrElse(defaultConfig.serialConsistency))
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

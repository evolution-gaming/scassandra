package com.evolution.scassandra

import cats.data.NonEmptyList
import cats.syntax.all.*
import pureconfig.error.CannotConvert
import pureconfig.module.cats.*
import pureconfig.{ConfigObjectCursor, ConfigReader}

/**
 * Replication strategy of a keyspace, see
 * [[https://cassandra.apache.org/doc/latest/cassandra/architecture/dynamo.html#replication-strategy]].
 *
 * Configuration format, same as in scassandra for the Java driver 3:
 * {{{
 * replication-strategy = "NetworkTopology" # or "Simple", the default
 * simple {
 *   replication-factor = 2
 * }
 * network-topology {
 *   replication-factors = ["dc1:2", "dc2:3"]
 * }
 * }}}
 */
sealed trait ReplicationStrategyConfig

object ReplicationStrategyConfig {

  val Default: ReplicationStrategyConfig = Simple.Default

  implicit val configReaderReplicationStrategyConfig: ConfigReader[ReplicationStrategyConfig] = {
    ConfigReader.fromCursor { cursor =>
      for {
        objectCursor <- cursor.asObjectCursor
        strategy <- optional[String](objectCursor, "replication-strategy")
        result <- strategy.map(_.toLowerCase) match {
          case None | Some("simple") =>
            optional[Simple](objectCursor, "simple").map(_.getOrElse(Simple.Default))
          case Some("networktopology") =>
            optional[NetworkTopology](
              objectCursor,
              "network-topology",
            ).map(_.getOrElse(NetworkTopology.Default))
          case Some(strategy) =>
            objectCursor.atKey("replication-strategy").flatMap { cursor =>
              cursor.failed(CannotConvert(
                strategy,
                "ReplicationStrategy",
                "expected Simple or NetworkTopology",
              ))
            }
        }
      } yield result
    }
  }

  implicit val toCqlReplicationStrategyConfig: ToCql[ReplicationStrategyConfig] = {
    case strategy: Simple =>
      s"'SimpleStrategy','replication_factor':${ strategy.replicationFactor }"

    case strategy: NetworkTopology =>
      val factors = strategy.replicationFactors
        .map { dcFactor => s"'${ dcFactor.name }':${ dcFactor.replicationFactor }" }
        .toList
        .mkString(",")
      s"'NetworkTopologyStrategy',$factors"
  }

  final case class Simple(replicationFactor: Int = 1) extends ReplicationStrategyConfig

  object Simple {

    val Default: Simple = Simple()

    implicit val configReaderSimple: ConfigReader[Simple] = ConfigReader.fromCursor { cursor =>
      for {
        objectCursor <- cursor.asObjectCursor
        replicationFactor <- optional[Int](objectCursor, "replication-factor")
      } yield Simple(replicationFactor = replicationFactor.getOrElse(Default.replicationFactor))
    }
  }

  final case class NetworkTopology(
    replicationFactors: NonEmptyList[NetworkTopology.DcFactor] = NonEmptyList.one(NetworkTopology.DcFactor()),
  ) extends ReplicationStrategyConfig

  object NetworkTopology {

    val Default: NetworkTopology = NetworkTopology()

    implicit val configReaderNetworkTopology: ConfigReader[NetworkTopology] = ConfigReader.fromCursor {
      cursor =>
        for {
          objectCursor <- cursor.asObjectCursor
          factorsCursor <- objectCursor.atKey("replication-factors")
          replicationFactors <- ConfigReader[NonEmptyList[DcFactor]].from(factorsCursor)
        } yield NetworkTopology(replicationFactors = replicationFactors)
    }

    final case class DcFactor(name: String = "localDc", replicationFactor: Int = 1)

    object DcFactor {

      implicit val configReaderDcFactor: ConfigReader[DcFactor] = ConfigReader[String].emap { value =>
        value.split(":").map(_.trim) match {
          case Array(name, factor) =>
            factor.toIntOption
              .map { factor => DcFactor(name, factor) }
              .toRight(CannotConvert(
                value,
                "DcFactor",
                s"replication factor should be a number, but is $factor",
              ))
          case _ =>
            CannotConvert(value, "DcFactor", "should be in form of [dc:replication-factor]").asLeft
        }
      }
    }
  }

  private def optional[A: ConfigReader](
    objectCursor: ConfigObjectCursor,
    key: String,
  ): ConfigReader.Result[Option[A]] = {
    ConfigReader[Option[A]].from(objectCursor.atKeyOrUndefined(key))
  }
}

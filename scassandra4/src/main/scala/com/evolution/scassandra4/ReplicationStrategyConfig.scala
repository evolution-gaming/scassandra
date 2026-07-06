package com.evolution.scassandra4

import cats.data.NonEmptyList
import com.evolution.scassandra4.util.PureconfigSyntax._
import com.typesafe.config.{Config, ConfigException}
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

/**
  * See [[https://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archDataDistributeReplication.html]]
  */
sealed trait ReplicationStrategyConfig

object ReplicationStrategyConfig {

  val Default: ReplicationStrategyConfig = Simple.Default

  implicit val configReaderReplicationStrategyConfig: ConfigReader[ReplicationStrategyConfig] = {
    (cursor: ConfigCursor) => {
      for {
        cursor <- cursor.asObjectCursor
      } yield {
        fromConfig(cursor.objValue.toConfig)
      }
    }
  }

  implicit val toCqlReplicationStrategyConfig: ToCql[ReplicationStrategyConfig] = {
    case a: Simple =>
      s"'SimpleStrategy','replication_factor':${ a.replicationFactor }"

    case a: NetworkTopology =>
      val factors = a.replicationFactors
        .map { dcFactor => s"'${ dcFactor.name }':${ dcFactor.replicationFactor }" }
        .toList
        .mkString(",")
      s"'NetworkTopologyStrategy',$factors"
  }


  def fromConfig(config: Config): ReplicationStrategyConfig = {

    val source = ConfigSource.fromConfig(config)

    def get[A: ConfigReader](name: String) = source.at(name).load[A]

    val strategy = get[String]("replication-strategy").toOption.map(_.toLowerCase).collect {
      case "simple"          => get[Simple]("simple") getOrElse Simple.Default
      case "networktopology" => get[NetworkTopology]("network-topology") getOrElse NetworkTopology.Default
    }

    strategy getOrElse Simple.Default
  }


  final case class Simple(replicationFactor: Int = 1) extends ReplicationStrategyConfig

  object Simple {

    val Default: Simple = Simple()

    implicit val configReaderSimple: ConfigReader[Simple] = ConfigReader.fromCursor[Simple] { cursor =>
      val defaultConfig = Simple()

      for {
        objCur <- cursor.asObjectCursor
        replicationFactor <- objCur.getAtOpt[Int]("replication-factor").map(_.getOrElse(defaultConfig.replicationFactor))
      } yield Simple(
        replicationFactor = replicationFactor
      )
    }
  }


  final case class NetworkTopology(
    replicationFactors: NonEmptyList[NetworkTopology.DcFactor] = NonEmptyList.of(NetworkTopology.DcFactor())) extends ReplicationStrategyConfig

  object NetworkTopology {

    val Default: NetworkTopology = NetworkTopology()

    implicit val configReaderNetworkTopology: ConfigReader[NetworkTopology] = {
      (cursor: ConfigCursor) => {
        for {
          cursor <- cursor.asObjectCursor
        } yield {
          fromConfig(cursor.objValue.toConfig)
        }
      }
    }


    private def fromConfig(config: Config): NetworkTopology = {

      val path = "replication-factors"
      val source = ConfigSource.fromConfig(config)

      val strings = {
        val fromList = source.at(path).load[List[String]]
        val fromString = source.at(path).load[String].map { _.split(",").toList.map(_.trim).filter(_.nonEmpty) }
        (fromList orElse fromString)
          .toOption
          .flatMap { NonEmptyList.fromList }
          .getOrElse {
            throw new ConfigException.BadValue(config.origin(), path, "Cannot parse non-empty list of DcFactor")
          }
      }

      val replicationFactors = strings.map { str =>
        str.split(":").map(_.trim) match {
          case Array(name, factor) => DcFactor(name, factor.toInt)
          case _                   => throw new ConfigException.BadValue(config.origin(), path, s"Cannot parse DcFactor from $str")
        }
      }

      NetworkTopology(replicationFactors = replicationFactors)
    }

    final case class DcFactor(name: String = "localDc", replicationFactor: Int = 1)
  }
}

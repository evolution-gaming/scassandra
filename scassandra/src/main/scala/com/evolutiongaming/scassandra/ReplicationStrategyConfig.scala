package com.evolutiongaming.scassandra

import com.evolutiongaming.config.ConfigHelper._
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ConfigHelpers._
import com.typesafe.config.{Config, ConfigException}
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

/**
  * See [[https://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archDataDistributeReplication.html]]
  */
sealed trait ReplicationStrategyConfig

object ReplicationStrategyConfig extends ReplicationStrategyConfigImplicits {

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
        .mkString(",")
      s"'NetworkTopologyStrategy',$factors"
  }

  @deprecated("use ConfigReader instead", "1.1.5")
  def apply(config: Config): ReplicationStrategyConfig = fromConfig(config)

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

    @deprecated("use ConfigReader instead", "1.1.5")
    def apply(config: Config): Simple = apply(config, Default)

    @deprecated("use ConfigReader instead", "1.1.5")
    def apply(config: Config, default: => Simple): Simple = {

      def get[A: FromConf](name: String) = config.getOpt[A](name)

      Simple(replicationFactor = get[Int]("replication-factor") getOrElse default.replicationFactor)
    }
  }


  final case class NetworkTopology(
    replicationFactors: Nel[NetworkTopology.DcFactor] = Nel(NetworkTopology.DcFactor())) extends ReplicationStrategyConfig

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


    @deprecated("use ConfigReader instead", "1.1.5")
    def apply(config: Config): NetworkTopology = fromConfig(config)


    private def fromConfig(config: Config): NetworkTopology = {
      val replicationFactors = {
        val path = "replication-factors"
        config.get[Nel[String]](path).map { str =>
          str.split(":").map(_.trim) match {
            case Array(name, factor) => DcFactor(name, factor.toInt)
            case str                 => throw new ConfigException.BadValue(config.origin(), path, s"Cannot parse DcFactor from $str")
          }
        }
      }

      NetworkTopology(replicationFactors = replicationFactors)
    }

    final case class DcFactor(name: String = "localDc", replicationFactor: Int = 1)
  }
}

package com.evolutiongaming.scassandra

import com.evolutiongaming.config.ConfigHelper._
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ConfigHelpers._
import com.typesafe.config.{Config, ConfigException}

/**
  * See [[https://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archDataDistributeReplication.html]]
  */
sealed trait ReplicationStrategyConfig

object ReplicationStrategyConfig {

  val Default: ReplicationStrategyConfig = Simple.Default

  implicit val ToCqlImpl: ToCql[ReplicationStrategyConfig] = new ToCql[ReplicationStrategyConfig] {
    
    def apply(a: ReplicationStrategyConfig) = a match {
      case a: Simple          =>
        s"'SimpleStrategy','replication_factor':${ a.replicationFactor }"
        
      case a: NetworkTopology =>
        val factors = a.replicationFactors
          .map { dcFactor => s"'${ dcFactor.name }':${ dcFactor.replicationFactor }" }
          .mkString(",")
        s"'NetworkTopologyStrategy',$factors"
    }
  }


  def apply(config: Config): ReplicationStrategyConfig = {

    def get[A: FromConf](name: String) = config.getOpt[A](name)

    val strategy = get[String]("replication-strategy").map(_.toLowerCase).collect {
      case "simple"          => get[Config]("simple").fold(Simple.Default)(Simple.apply)
      case "networktopology" => get[Config]("network-topology").fold(NetworkTopology.Default)(NetworkTopology.apply)
    }

    strategy getOrElse Simple.Default
  }


  final case class Simple(replicationFactor: Int = 1) extends ReplicationStrategyConfig

  object Simple {
    val Default: Simple = Simple()

    def apply(config: Config): Simple = apply(config, Default)

    def apply(config: Config, default: => Simple): Simple = {

      def get[A: FromConf](name: String) = config.getOpt[A](name)

      Simple(replicationFactor = get[Int]("replication-factor") getOrElse default.replicationFactor)
    }
  }


  final case class NetworkTopology(
    replicationFactors: Nel[NetworkTopology.DcFactor] = Nel(NetworkTopology.DcFactor())) extends ReplicationStrategyConfig

  object NetworkTopology {

    val Default: NetworkTopology = NetworkTopology()

    def apply(config: Config): NetworkTopology = {
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

package com.evolutiongaming.scassandra

import com.datastax.driver.core.ProtocolOptions.Compression
import com.datastax.driver.core.ProtocolVersion
import com.evolutiongaming.config.ConfigHelper._
import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ConfigHelpers._
import com.typesafe.config.{Config, ConfigException}

/**
  * See [[https://docs.datastax.com/en/developer/java-driver/3.5/manual/#setting-up-the-driver]]
  */
final case class CassandraConfig(
  name: String = "cluster",
  port: Int = 9042,
  contactPoints: Nel[String] = Nel("127.0.0.1"),
  protocolVersion: Option[ProtocolVersion] = None,
  pooling: PoolingConfig = PoolingConfig.Default,
  query: QueryConfig = QueryConfig.Default,
  reconnection: ReconnectionConfig = ReconnectionConfig.Default,
  socket: SocketConfig = SocketConfig.Default,
  authentication: Option[AuthenticationConfig] = None,
  loadBalancing: Option[LoadBalancingConfig] = None,
  speculativeExecution: Option[SpeculativeExecutionConfig] = None,
  compression: Compression = Compression.NONE,
  logQueries: Boolean = false)


object CassandraConfig {

  val Default: CassandraConfig = CassandraConfig()

  private implicit val CompressionFromConf = FromConf[Compression] { (conf, path) =>
    val str = conf.getString(path)
    val value = Compression.values().find { _.name equalsIgnoreCase str }
    value getOrElse {
      throw new ConfigException.BadValue(conf.origin(), path, s"Cannot parse Compression from $str")
    }
  }

  private implicit val ProtocolVersionFromConf = FromConf[ProtocolVersion] { (conf, path) =>
    val str = conf.getString(path)
    val value = ProtocolVersion.values().find { _.name equalsIgnoreCase str }
    value getOrElse {
      throw new ConfigException.BadValue(conf.origin(), path, s"Cannot parse ProtocolVersion from $str")
    }
  }


  def apply(config: Config): CassandraConfig = apply(config, Default)

  def apply(config: Config, default: => CassandraConfig): CassandraConfig = {

    def get[A: FromConf](name: String) = config.getOpt[A](name)

    val pooling = get[Config]("pooling").fold(default.pooling)(PoolingConfig.apply(_, default.pooling))
    val query = get[Config]("query").fold(default.query)(QueryConfig.apply(_, default.query))
    val reconnection = get[Config]("reconnection").fold(default.reconnection)(ReconnectionConfig.apply(_, default.reconnection))
    val socket = get[Config]("socket").fold(default.socket)(SocketConfig.apply(_, default.socket))
    val authentication = get[Config]("authentication").map(AuthenticationConfig.apply)
    val loadBalancing = get[Config]("load-balancing").map { config =>
      default.loadBalancing.fold(LoadBalancingConfig(config)) { loadBalancing => LoadBalancingConfig(config, loadBalancing) }
    }
    val speculativeExecution = get[Config]("speculative-execution").map { config =>
      default.speculativeExecution.fold(SpeculativeExecutionConfig(config)) { speculativeExecution =>
        SpeculativeExecutionConfig(config, speculativeExecution)
      }
    }

    CassandraConfig(
      name = get[String]("name") getOrElse default.name,
      port = get[Int]("port") getOrElse default.port,
      contactPoints = get[Nel[String]]("contact-points") getOrElse default.contactPoints,
      protocolVersion = get[ProtocolVersion]("protocol-version"),
      pooling = pooling,
      query = query,
      reconnection = reconnection,
      socket = socket,
      authentication = authentication,
      loadBalancing = loadBalancing,
      speculativeExecution = speculativeExecution,
      compression = get[Compression]("compression") getOrElse default.compression,
      logQueries = get[Boolean]("log-queries") getOrElse default.logQueries)
  }
}

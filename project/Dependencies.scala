import sbt._

object Dependencies {

  val scalatest            = "org.scalatest"          %% "scalatest"             % "3.0.7"
  val `executor-tools`     = "com.evolutiongaming"    %% "executor-tools"        % "1.0.1"
  val `config-tools`       = "com.evolutiongaming"    %% "config-tools"          % "1.0.3"
  val nel                  = "com.evolutiongaming"    %% "nel"                   % "1.3.3"
  val `future-helper`      = "com.evolutiongaming"    %% "future-helper"         % "1.0.4"
  val `cassandra-launcher` = "com.evolutiongaming"    %% "cassandra-launcher"    % "0.0.2"
  val `cassandra-driver`   = "com.datastax.cassandra"  % "cassandra-driver-core" % "3.7.1"

  object Logback {
    private val version = "1.2.3"
    val core    = "ch.qos.logback" % "logback-core"    % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object Slf4j {
    private val version = "1.7.26"
    val api                = "org.slf4j" % "slf4j-api"        % version
    val `log4j-over-slf4j` = "org.slf4j" % "log4j-over-slf4j" % version
  }
}
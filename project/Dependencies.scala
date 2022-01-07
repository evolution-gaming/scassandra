import sbt._

object Dependencies {

  val `cassandra-driver`   = "com.datastax.cassandra"  % "cassandra-driver-core" % "3.11.0"
  val scalatest            = "org.scalatest"          %% "scalatest"             % "3.2.10"
  val `executor-tools`     = "com.evolutiongaming"    %% "executor-tools"        % "1.0.3"
  val `config-tools`       = "com.evolutiongaming"    %% "config-tools"          % "1.0.4"
  val nel                  = "com.evolutiongaming"    %% "nel"                   % "1.3.4"
  val `cassandra-launcher` = "com.evolutiongaming"    %% "cassandra-launcher"    % "0.0.4"
  val `cats-helper`        = "com.evolutiongaming"    %% "cats-helper"           % "2.7.2"
  val sstream              = "com.evolutiongaming"    %% "sstream"               % "0.2.1"

  object Logback {
    private val version = "1.2.7"
    val core    = "ch.qos.logback" % "logback-core"    % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object Slf4j {
    private val version = "1.7.32"
    val api                = "org.slf4j" % "slf4j-api"        % version
    val `log4j-over-slf4j` = "org.slf4j" % "log4j-over-slf4j" % version
  }

  object Cats {
    val core   = "org.typelevel" %% "cats-core"   % "2.7.0"
    val effect = "org.typelevel" %% "cats-effect" % "2.5.4"
  }

  object Pureconfig {
    private val version = "0.12.3"
    val pureconfig = "com.github.pureconfig" %% "pureconfig"      % version
    val cats       = "com.github.pureconfig" %% "pureconfig-cats" % version
  }
}
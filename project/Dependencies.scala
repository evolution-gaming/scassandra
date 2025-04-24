import sbt._

object Dependencies {

  val `cassandra-driver`         = "com.datastax.cassandra"  % "cassandra-driver-core"           % "3.11.3"
  val scalatest                  = "org.scalatest"          %% "scalatest"                       % "3.2.19"
  val `executor-tools`           = "com.evolutiongaming"    %% "executor-tools"                  % "1.0.4"
  val `config-tools`             = "com.evolutiongaming"    %% "config-tools"                    % "1.0.5"
  val nel                        = "com.evolutiongaming"    %% "nel"                             % "1.3.5"
  val `testcontainers-cassandra` = "com.dimafeng"           %% "testcontainers-scala-cassandra"  % "0.40.17"
  val `cats-helper`              = "com.evolutiongaming"    %% "cats-helper"                     % "3.9.0"
  val sstream                    = "com.evolutiongaming"    %% "sstream"                         % "1.0.2"

  object Logback {
    private val version = "1.4.11"
    val core    = "ch.qos.logback" % "logback-core"    % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object Slf4j {
    private val version = "2.0.9"
    val api                = "org.slf4j" % "slf4j-api"        % version
    val `log4j-over-slf4j` = "org.slf4j" % "log4j-over-slf4j" % version
  }

  object Cats {
    val core   = "org.typelevel" %% "cats-core"   % "2.10.0"
    val effect = "org.typelevel" %% "cats-effect" % "3.4.8"
  }

  object Pureconfig {
    private val version = "0.17.9"

    val core       = "com.github.pureconfig" %% "pureconfig-core" % version
    val pureconfig = "com.github.pureconfig" %% "pureconfig"      % version
    val cats       = "com.github.pureconfig" %% "pureconfig-cats" % version
  }
}

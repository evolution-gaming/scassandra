import sbt._

object Dependencies {

  val `cassandra-driver`         = "com.datastax.cassandra"  % "cassandra-driver-core"           % "3.11.5"
  // upgrade to version, which doesn't use JDK's `Unsafe` (https://github.com/jnr/jffi/issues/165#issuecomment-3444932263)
  val jffi                       = "com.github.jnr"          % "jffi"                            % "1.3.14"

  val scalatest                  = "org.scalatest"          %% "scalatest"                       % "3.2.19"
  val `executor-tools`           = "com.evolutiongaming"    %% "executor-tools"                  % "1.0.5"
  val `config-tools`             = "com.evolutiongaming"    %% "config-tools"                    % "1.0.5"
  val nel                        = "com.evolutiongaming"    %% "nel"                             % "1.3.5"
  val `testcontainers-cassandra` = "com.dimafeng"           %% "testcontainers-scala-cassandra"  % "0.43.6"
  val `cats-helper`              = "com.evolutiongaming"    %% "cats-helper"                     % "3.9.0"
  val sstream                    = "com.evolutiongaming"    %% "sstream"                         % "1.1.0"

  object Logback {
    private val version = "1.4.14"
    val core    = "ch.qos.logback" % "logback-core"    % version
    val classic = "ch.qos.logback" % "logback-classic" % version
  }

  object Slf4j {
    private val version = "2.0.17"
    val api                = "org.slf4j" % "slf4j-api"        % version
    val `log4j-over-slf4j` = "org.slf4j" % "log4j-over-slf4j" % version
  }

  object Cats {
    val core   = "org.typelevel" %% "cats-core"   % "2.13.0"
    val effect = "org.typelevel" %% "cats-effect" % "3.4.8"
  }

  object Pureconfig {
    private val version = "0.17.6"

    val core       = "com.github.pureconfig" %% "pureconfig-core" % version
    val pureconfig = "com.github.pureconfig" %% "pureconfig"      % version
    val cats       = "com.github.pureconfig" %% "pureconfig-cats" % version
  }
}

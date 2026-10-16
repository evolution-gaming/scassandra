package com.evolution.scassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.error.ConfigReaderFailures
import pureconfig.{ConfigReader, ConfigSource}

/**
 * Configuration of the Java driver 4, i.e. the contents of the `datastax-java-driver`
 * section described in
 * [[https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/configuration/reference/]].
 *
 * Unlike the default loading mechanism of the driver, the configuration could be read
 * from any path, which allows several sessions with different configurations in the same
 * application.
 *
 * @param driver
 *   driver configuration with the `datastax-java-driver` root stripped, the missing
 *   options are taken from the `reference.conf` of the driver
 * @param warnings
 *   issues found when producing the configuration, i.e. the ignored fields of the
 *   configuration of scassandra for the Java driver 3 translated by
 *   `scassandra-v4-compat`, logged when a session is created
 */
final case class CassandraConfig(driver: Config, warnings: List[String] = List.empty) {

  /**
   * Creates a new loader of this configuration for a session.
   *
   * Reloading is disabled as the configuration is fixed. Pass a session builder with a
   * custom loader to `CassandraSession.make` if reloading is required.
   */
  def configLoader: DriverConfigLoader = {
    val config = driver.withFallback(CassandraConfig.reference)
    new DefaultDriverConfigLoader(() => config, false)
  }

  // the configuration might contain credentials
  override def toString: String = s"CassandraConfig(driver = ***, warnings = $warnings)"
}

object CassandraConfig {

  /**
   * The path used by the Java driver 4 to read its configuration by default
   */
  val DefaultPath: String = DefaultDriverConfigLoader.DEFAULT_ROOT_PATH

  /**
   * The defaults of the Java driver 4, i.e. its `reference.conf`.
   *
   * These differ from the defaults of scassandra for the Java driver 3, e.g. a 2s request
   * timeout instead of 12s, see `docs/legacy-config-translation.md`.
   */
  val Default: CassandraConfig = CassandraConfig(ConfigFactory.empty())

  private lazy val reference: Config = {
    ConfigFactory
      .defaultReference(classOf[CqlSession].getClassLoader)
      .getConfig(DefaultPath)
  }

  /**
   * Reads the configuration of the driver, the cursor should point to the
   * `datastax-java-driver` section or its equivalent.
   *
   * The missing options get the defaults of the Java driver 4, see [[Default]]. Use
   * `scassandra-v4-compat` to read the configuration of scassandra for the Java driver 3
   * with its defaults.
   */
  implicit val configReaderCassandraConfig: ConfigReader[CassandraConfig] = {
    ConfigReader[Config].map { driver => CassandraConfig(driver) }
  }

  /**
   * Reads the configuration of the driver from the given path.
   *
   * The missing options get the defaults of the Java driver 4, see [[Default]]. Use
   * `scassandra-v4-compat` to read the configuration of scassandra for the Java driver 3
   * with its defaults.
   *
   * @param path
   *   path to the configuration of the driver, i.e. `datastax-java-driver` or
   *   `my-service.cassandra`
   */
  def fromConfig(config: Config, path: String = DefaultPath)
    : Either[ConfigReaderFailures, CassandraConfig] = {
    ConfigSource.fromConfig(config).at(path).load[CassandraConfig]
  }
}

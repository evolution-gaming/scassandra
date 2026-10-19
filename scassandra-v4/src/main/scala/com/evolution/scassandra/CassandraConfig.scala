package com.evolution.scassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions, ConfigUtil, ConfigValueFactory}
import pureconfig.error.ConfigReaderFailures
import pureconfig.{ConfigReader, ConfigSource}

import scala.jdk.CollectionConverters.*

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
 */
final case class CassandraConfig(driver: Config) {

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

  /**
   * Prints the configuration with the values of the keys looking like secrets masked,
   * e.g. `advanced.auth-provider.password`
   */
  override def toString: String = {
    s"CassandraConfig(driver = ${ CassandraConfig.masked(driver).root.render(ConfigRenderOptions.concise) })"
  }
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

  private val SecretKeyParts: List[String] = List("password", "secret", "token", "credential")

  private def masked(config: Config): Config = {
    config.entrySet.asScala.map(_.getKey).filter(isSecret).foldLeft(config) { (config, path) =>
      config.withValue(path, ConfigValueFactory.fromAnyRef("***"))
    }
  }

  private def isSecret(path: String): Boolean = {
    val key = ConfigUtil.splitPath(path).asScala.lastOption.getOrElse(path).toLowerCase
    SecretKeyParts.exists(key.contains)
  }

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

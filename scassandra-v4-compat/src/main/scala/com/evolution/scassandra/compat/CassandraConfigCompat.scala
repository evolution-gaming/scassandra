package com.evolution.scassandra.compat

import cats.syntax.all.*
import com.evolution.scassandra.CassandraConfig
import com.evolutiongaming.scassandra.CassandraConfig as CassandraConfigV3
import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigUtil}
import pureconfig.error.{ConfigReaderFailures, ThrowableFailure}
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

/**
 * Translates the configuration of scassandra for the Java driver 3
 * (`com.evolutiongaming:scassandra`) into the configuration of the Java driver 4.
 *
 * See `docs/legacy-config-translation.md` for the details on how each field is
 * translated.
 */
object CassandraConfigCompat {

  /**
   * The key under the legacy configuration, containing the configuration of the driver 4,
   * applied on top of the translated one
   */
  val LegacyDriverKey: String = "driver"

  /**
   * Translates the configuration of scassandra for the Java driver 3.
   *
   * The ignored fields are returned as `TranslatedConfig.warnings`, for the caller to
   * log.
   *
   * The fields keep their values, including the defaults of scassandra for the Java
   * driver 3, which differ from the defaults of the Java driver 4, e.g. a 12s request
   * timeout instead of 2s. Only the options not covered by the translation get the
   * defaults of the Java driver 4.
   *
   * @param config
   *   configuration of scassandra for the Java driver 3
   * @param driverOverrides
   *   configuration of the Java driver 4, applied on top of the translated one
   */
  def fromV3(
    config: CassandraConfigV3,
    driverOverrides: Config = ConfigFactory.empty(),
  ): Either[ConfigReaderFailures, TranslatedConfig] = {
    translate(config, driverOverrides, path = None)
  }

  /**
   * Reads the configuration of scassandra for the Java driver 3 from the given path, with
   * the configuration of the driver 4 at `<path>.driver`, applied on top of the
   * translated one.
   *
   * The configuration of scassandra for the Java driver 3 is read by its own reader, so
   * the values are interpreted exactly as before, and the missing keys get the defaults
   * of scassandra for the Java driver 3, e.g. a 12s request timeout instead of the 2s
   * default of the Java driver 4.
   *
   * Allows keeping the existing configuration as is, while moving to the configuration of
   * the driver 4 gradually. Once the migration is complete, read the configuration with
   * `com.evolution.scassandra.CassandraConfig.fromConfig(config, "<path>.driver")`. Note,
   * that it applies the defaults of the Java driver 4 to the options missing under
   * `<path>.driver`, so set every option which should keep the value it had with
   * scassandra for the Java driver 3.
   */
  def fromLegacyConfig(config: Config, path: String): Either[ConfigReaderFailures, TranslatedConfig] = {
    for {
      section <- ConfigSource.fromConfig(config).at(path).load[Config]
      result <- fromLegacySection(section, path.some)
    } yield result
  }

  /**
   * Same as [[fromLegacyConfig]], for reading the configuration with pureconfig at any
   * path, with the defaults of scassandra for the Java driver 3 as well.
   */
  val legacyConfigReader: ConfigReader[TranslatedConfig] = ConfigReader.fromCursor { cursor =>
    for {
      objectCursor <- cursor.asObjectCursor
      result <- fromLegacySection(objectCursor.objValue.toConfig, pathOf(cursor))
    } yield result
  }

  private def fromLegacySection(
    section: Config,
    path: Option[String],
  ): Either[ConfigReaderFailures, TranslatedConfig] = {
    val source = ConfigSource.fromConfig(section)
    for {
      config <- readV3(source)
      overrides <-
        if (section.hasPath(LegacyDriverKey)) source.at(LegacyDriverKey).load[Config]
        else ConfigFactory.empty().asRight
      result <- translate(config, overrides, path)
    } yield result
  }

  // the reader of scassandra for the driver 3 throws on some invalid values, i.e. empty contact points
  private def readV3(source: ConfigSource): Either[ConfigReaderFailures, CassandraConfigV3] = {
    try {
      source.load[CassandraConfigV3]
    } catch {
      case e: ConfigException => ConfigReaderFailures(ThrowableFailure(e, Option(e.origin()))).asLeft
    }
  }

  private def translate(
    config: CassandraConfigV3,
    overrides: Config,
    path: Option[String],
  ): Either[ConfigReaderFailures, TranslatedConfig] = {
    V3Translation(config, overrides, path).map { translated =>
      TranslatedConfig(CassandraConfig(overrides.withFallback(translated.driver)), translated.warnings)
    }
  }

  private def pathOf(cursor: ConfigCursor): Option[String] = {
    cursor.pathElems match {
      case Nil => none
      // the keys are in reverse order, the deepest one first
      case pathElems => ConfigUtil.joinPath(pathElems.reverse*).some
    }
  }
}

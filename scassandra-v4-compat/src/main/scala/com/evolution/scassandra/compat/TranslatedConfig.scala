package com.evolution.scassandra.compat

import com.evolution.scassandra.CassandraConfig

/**
 * Configuration of the Java driver 4, translated from the configuration of scassandra for
 * the Java driver 3.
 *
 * @param config
 *   the translated configuration, to create a session with
 * @param warnings
 *   the fields of the configuration of scassandra for the Java driver 3, which have no
 *   equivalent in the Java driver 4 and are ignored, see
 *   `docs/legacy-config-translation.md`; they are not logged, it is up to the caller
 */
final case class TranslatedConfig(config: CassandraConfig, warnings: List[String])

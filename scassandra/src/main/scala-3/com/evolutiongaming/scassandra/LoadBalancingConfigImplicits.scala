package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

trait LoadBalancingConfigImplicits {
  implicit val configReaderLoadBalancingConfig: ConfigReader[LoadBalancingConfig] = ConfigReader.derived
}
package com.evolutiongaming.scassandra

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

trait LoadBalancingConfigImplicits {
  implicit val configReaderLoadBalancingConfig: ConfigReader[LoadBalancingConfig] = deriveReader
}
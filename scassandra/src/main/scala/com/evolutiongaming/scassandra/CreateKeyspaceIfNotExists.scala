package com.evolutiongaming.scassandra

import com.evolutiongaming.scassandra.syntax._

object CreateKeyspaceIfNotExists {

  def apply(name: String, replicationStrategy: ReplicationStrategyConfig): String = {
    s"CREATE KEYSPACE IF NOT EXISTS $name WITH REPLICATION = { 'class' : ${ replicationStrategy.toCql } }"
  }
}
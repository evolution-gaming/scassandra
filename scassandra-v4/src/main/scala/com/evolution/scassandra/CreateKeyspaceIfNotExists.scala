package com.evolution.scassandra

import com.evolution.scassandra.syntax.*

object CreateKeyspaceIfNotExists {

  def apply(name: String, replicationStrategy: ReplicationStrategyConfig): String = {
    s"CREATE KEYSPACE IF NOT EXISTS $name WITH REPLICATION = { 'class' : ${ replicationStrategy.toCql } }"
  }
}

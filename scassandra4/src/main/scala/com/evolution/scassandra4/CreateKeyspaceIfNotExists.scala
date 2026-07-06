package com.evolution.scassandra4

import com.evolution.scassandra4.syntax._

object CreateKeyspaceIfNotExists {

  def apply(name: String, replicationStrategy: ReplicationStrategyConfig): String = {
    s"CREATE KEYSPACE IF NOT EXISTS $name WITH REPLICATION = { 'class' : ${ replicationStrategy.toCql } }"
  }
}

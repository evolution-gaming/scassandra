package com.evolutiongaming.scassandra

import com.evolutiongaming.nel.Nel
import com.evolutiongaming.scassandra.ReplicationStrategyConfig.{NetworkTopology, Simple}
import com.evolutiongaming.scassandra.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CreateKeyspaceIfNotExistsSpec extends AnyFunSuite with Matchers {

  test("simple strategy") {
    CreateKeyspaceIfNotExists("ks", Simple(2)) shouldEqual
      "CREATE KEYSPACE IF NOT EXISTS ks WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':2 }"
  }

  test("network topology strategy") {
    val strategy =
      NetworkTopology(Nel(NetworkTopology.DcFactor("dc1", 2), NetworkTopology.DcFactor("dc2", 3)))
    CreateKeyspaceIfNotExists("ks", strategy) shouldEqual
      "CREATE KEYSPACE IF NOT EXISTS ks WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy','dc1':2,'dc2':3 }"
  }

  test("default strategy") {
    CreateKeyspaceIfNotExists("ks", ReplicationStrategyConfig.Default) shouldEqual
      "CREATE KEYSPACE IF NOT EXISTS ks WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 }"
  }

  test("TableName.toCql") {
    TableName("ks", "table").toCql shouldEqual "ks.table"
    ToCql[TableName].apply(TableName("ks", "table")) shouldEqual "ks.table"
  }
}

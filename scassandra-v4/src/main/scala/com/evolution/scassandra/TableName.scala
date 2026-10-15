package com.evolution.scassandra

final case class TableName(keyspace: String, table: String)

object TableName {

  implicit val toCqlTableName: ToCql[TableName] =
    (tableName: TableName) => s"${ tableName.keyspace }.${ tableName.table }"
}
